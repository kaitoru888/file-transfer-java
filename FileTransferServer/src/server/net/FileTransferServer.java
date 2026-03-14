package server.net;

import proto.Protocol;
import server.config.AppConfig;
import server.dao.FileDao;
import server.dao.LogDao;
import server.dao.UserDao;
import server.security.PasswordUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileTransferServer {

    private final UserDao userDao = new UserDao();
    private final FileDao fileDao = new FileDao();
    private final LogDao logDao = new LogDao();

    private final Map<String, ClientSession> online = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private volatile boolean running;

    public interface Listener {
        void onLog(String s);
        void onOnlineChanged(int count);
    }

    private Listener listener;

    public void setListener(Listener l){ this.listener = l; }

    public int onlineCount(){ return online.size(); }

    public synchronized void start(int port) throws Exception {
        if (running) return;
        running = true;

        Files.createDirectories(AppConfig.STORAGE_DIR);
        serverSocket = new ServerSocket(port);

        log("Server started on port " + port);

        Thread accept = new Thread(() -> {
            while (running) {
                try {
                    Socket s = serverSocket.accept();
                    Thread t = new Thread(() -> handleClient(s));
                    t.setDaemon(true);
                    t.start();
                } catch (Exception e) {
                    if (running) log("Accept error: " + e.getMessage());
                }
            }
        }, "accept-thread");
        accept.setDaemon(true);
        accept.start();
    }

    public synchronized void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        online.values().forEach(sess -> { try { sess.socket.close(); } catch (Exception ignored) {} });
        online.clear();
        fireOnlineChanged();
        log("Server stopped");
    }

    private void handleClient(Socket socket) {
        String currentUser = null;
        Integer currentUserId = null;

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            while (running && !socket.isClosed()) {
                int op;
                try {
                    op = in.readInt();
                } catch (EOFException eof) {
                    break;
                }

                if (op == Protocol.LOGIN) {
                    String username = in.readUTF();
                    String password = in.readUTF();

                    var opt = userDao.findByUsername(username);
                    boolean ok = opt.isPresent()
                            && "ACTIVE".equalsIgnoreCase(opt.get().status)
                            && PasswordUtil.verify(password, opt.get().passwordHash);

                    out.writeInt(Protocol.LOGIN_RESULT);
                    out.writeBoolean(ok);
                    out.writeUTF(ok ? "OK" : "Sai tài khoản/mật khẩu hoặc bị khóa");
                    out.flush();

                    if (ok) {
                        currentUser = username;
                        currentUserId = opt.get().userId;
                        online.put(username, new ClientSession(username, currentUserId, socket, out));
                        fireOnlineChanged();
                        log("LOGIN OK: " + username);
                        logDao.insert("LOGIN", null, currentUserId, null, "OK", "login");
                    } else {
                        log("LOGIN FAIL: " + username);
                        logDao.insert("LOGIN", null, null, null, "FAIL", "login fail: " + username);
                    }
                }

                else if (op == Protocol.LIST_USERS) {
                    if (!isAuthed(currentUserId, out)) continue;

                    var all = userDao.listAllActiveUsernames();
                    out.writeInt(Protocol.USERS_RESULT);
                    out.writeInt(all.size());
                    for (String u : all) {
                        out.writeUTF(u);
                        out.writeBoolean(online.containsKey(u));
                    }
                    out.flush();
                }

                else if (op == Protocol.SEND_FILE) {
                    if (!isAuthed(currentUserId, out)) continue;

                    String receiver = in.readUTF();
                    String fileName = in.readUTF();
                    long fileSize = in.readLong();

                    int senderId = currentUserId;
                    int receiverId;
                    try {
                        receiverId = userDao.idByUsername(receiver);
                    } catch (Exception ex) {
                        skipN(in, fileSize);
                        sendResult(out, false, "Người nhận không tồn tại");
                        logDao.insert("SEND_FILE", null, senderId, null, "FAIL", "Receiver not found: " + receiver);
                        continue;
                    }

                    Path saved = saveIncomingFile(in, fileName, senderId, receiverId, fileSize);
                    int fileId = fileDao.insert(fileName, fileSize, senderId, receiverId, saved.toString(), "SENT");
                    logDao.insert("SEND_FILE", fileId, senderId, receiverId, "OK", "Saved to: " + saved);

                    // push nếu receiver online
                    ClientSession recvSess = online.get(receiver);
                    if (recvSess != null) {
                        boolean pushed = pushFileToReceiver(recvSess, currentUser, fileId, fileName, fileSize, saved);
                        if (pushed) {
                            fileDao.updateStatus(fileId, "RECEIVED");
                            logDao.insert("PUSH_FILE", fileId, senderId, receiverId, "OK", "Delivered online");
                        } else {
                            logDao.insert("PUSH_FILE", fileId, senderId, receiverId, "FAIL", "Push failed -> keep pending");
                        }
                    }

                    sendResult(out, true, "Đã gửi (nếu người nhận online sẽ nhận ngay)");
                }

                else if (op == Protocol.PULL_PENDING) {
                    if (!isAuthed(currentUserId, out)) continue;

                    var pending = fileDao.pendingForReceiver(currentUserId);

                    out.writeInt(Protocol.PENDING_RESULT);
                    out.writeInt(pending.size());

                    for (var fr : pending) {
                        out.writeInt(fr.fileId);
                        out.writeUTF(fr.senderUsername);
                        out.writeUTF(fr.fileName);
                        out.writeLong(fr.fileSize);

                        Path p = Path.of(fr.serverPath);
                        try (InputStream fis = Files.newInputStream(p)) {
                            transferBytes(fis, out, fr.fileSize);
                        }
                    }
                    out.flush();

                    for (var fr : pending) {
                        fileDao.updateStatus(fr.fileId, "RECEIVED");
                        logDao.insert("PULL_PENDING", fr.fileId, null, currentUserId, "OK", "Client pulled pending");
                    }
                }

                else if (op == Protocol.HISTORY) {
                    if (!isAuthed(currentUserId, out)) continue;

                    String keyword = in.readUTF();
                    var list = fileDao.historyForUser(currentUserId);

                    String k = (keyword == null) ? "" : keyword.trim().toLowerCase();

                    out.writeInt(Protocol.HISTORY_RESULT);
                    int count = 0;
                    for (var fr : list) {
                        if (k.isEmpty()
                                || fr.fileName.toLowerCase().contains(k)
                                || fr.senderUsername.toLowerCase().contains(k)
                                || fr.receiverUsername.toLowerCase().contains(k)) count++;
                    }
                    out.writeInt(count);

                    for (var fr : list) {
                        if (!k.isEmpty()
                                && !(fr.fileName.toLowerCase().contains(k)
                                || fr.senderUsername.toLowerCase().contains(k)
                                || fr.receiverUsername.toLowerCase().contains(k))) continue;

                        out.writeInt(fr.fileId);
                        out.writeUTF(fr.fileName);
                        out.writeLong(fr.fileSize);
                        out.writeUTF(fr.senderUsername);
                        out.writeUTF(fr.receiverUsername);
                        out.writeUTF(fr.status);
                        out.writeUTF(fr.sentAt == null ? "" : fr.sentAt.toString());
                    }
                    out.flush();
                }

                else if (op == Protocol.LOGOUT) {
                    break;
                }

                else {
                    out.writeInt(Protocol.ERROR);
                    out.writeUTF("Unknown opcode: " + op);
                    out.flush();
                }
            }

        } catch (Exception e) {
            log("Client error: " + e.getMessage());
        } finally {
            if (currentUser != null) online.remove(currentUser);
            fireOnlineChanged();
            try { socket.close(); } catch (Exception ignored) {}
            if (currentUser != null) log("Disconnected: " + currentUser);
        }
    }

    private boolean isAuthed(Integer userId, DataOutputStream out) throws IOException {
        if (userId != null) return true;
        out.writeInt(Protocol.ERROR);
        out.writeUTF("Not authenticated");
        out.flush();
        return false;
    }

    private void sendResult(DataOutputStream out, boolean ok, String msg) throws IOException {
        out.writeInt(Protocol.SEND_RESULT);
        out.writeBoolean(ok);
        out.writeUTF(msg);
        out.flush();
    }

    private Path saveIncomingFile(DataInputStream in, String fileName, int senderId, int receiverId, long size) throws IOException {
        String safeName = fileName.replaceAll("[\\\\/\\n\\r\\t]", "_");
        String stamp = System.currentTimeMillis() + "_" + senderId + "_" + receiverId;
        Path dir = AppConfig.STORAGE_DIR;
        Files.createDirectories(dir);
        Path target = dir.resolve(stamp + "_" + safeName);

        try (OutputStream fos = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            transferBytes(in, fos, size);
        }
        return target;
    }

    private boolean pushFileToReceiver(ClientSession recvSess, String senderUsername, int fileId,
                                       String fileName, long fileSize, Path serverPath) {
        synchronized (recvSess.out) {
            try {
                recvSess.out.writeInt(Protocol.FILE_INCOMING);
                recvSess.out.writeInt(fileId);
                recvSess.out.writeUTF(senderUsername);
                recvSess.out.writeUTF(fileName);
                recvSess.out.writeLong(fileSize);

                try (InputStream fis = Files.newInputStream(serverPath)) {
                    transferBytes(fis, recvSess.out, fileSize);
                }
                recvSess.out.flush();
                return true;
            } catch (Exception e) {
                log("Push error to " + recvSess.username + ": " + e.getMessage());
                return false;
            }
        }
    }

    private void transferBytes(InputStream in, OutputStream out, long size) throws IOException {
        byte[] buf = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            int read = in.read(buf, 0, (int)Math.min(buf.length, remaining));
            if (read == -1) throw new EOFException("Unexpected EOF while reading bytes");
            out.write(buf, 0, read);
            remaining -= read;
        }
    }

    private void transferBytes(DataInputStream in, OutputStream out, long size) throws IOException {
        byte[] buf = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            int read = in.read(buf, 0, (int)Math.min(buf.length, remaining));
            if (read == -1) throw new EOFException("Unexpected EOF while reading bytes");
            out.write(buf, 0, read);
            remaining -= read;
        }
    }

    private void skipN(DataInputStream in, long size) throws IOException {
        byte[] buf = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            int read = in.read(buf, 0, (int)Math.min(buf.length, remaining));
            if (read == -1) break;
            remaining -= read;
        }
    }

    private void log(String s){
        if (listener != null) listener.onLog(s);
        else System.out.println(s);
    }

    private void fireOnlineChanged(){
        if (listener != null) listener.onOnlineChanged(online.size());
    }
}