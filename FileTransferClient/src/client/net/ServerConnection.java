package client.net;

import proto.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerConnection {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;

    private final BlockingQueue<Response> responses = new LinkedBlockingQueue<>();

    public interface IncomingFileListener {
        void onIncomingFile(int fileId, String sender, String fileName, long size, byte[] bytes);
    }

    private IncomingFileListener incomingFileListener;

    public void setIncomingFileListener(IncomingFileListener l) { this.incomingFileListener = l; }

    public String getUsername() { return username; }

    public void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        startReaderThread();
    }

    public void close() {
        try {
            synchronized (out) {
                out.writeInt(Protocol.LOGOUT);
                out.flush();
            }
        } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    // ===== API =====

    public synchronized LoginResult login(String user, String pass) throws Exception {
        send(opWriter -> {
            opWriter.writeInt(Protocol.LOGIN);
            opWriter.writeUTF(user);
            opWriter.writeUTF(pass);
        });

        Response r = waitFor(Protocol.LOGIN_RESULT);
        boolean ok = (boolean) r.payload;
        String msg = r.message;
        if (ok) this.username = user;
        return new LoginResult(ok, msg);
    }

    public synchronized List<UserOnline> listUsers() throws Exception {
        send(w -> {
            w.writeInt(Protocol.LIST_USERS);
        });

        Response r = waitFor(Protocol.USERS_RESULT);
        @SuppressWarnings("unchecked")
        List<UserOnline> list = (List<UserOnline>) r.payload;
        return list;
    }

    public synchronized SendResult sendFile(String receiver, File file, ProgressListener progress) throws Exception {
        long size = file.length();

        send(w -> {
            w.writeInt(Protocol.SEND_FILE);
            w.writeUTF(receiver);
            w.writeUTF(file.getName());
            w.writeLong(size);

            try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buf = new byte[8192];
                long sent = 0;
                while (sent < size) {
                    int read = fis.read(buf);
                    if (read == -1) break;
                    w.write(buf, 0, read);
                    sent += read;
                    if (progress != null) progress.onProgress(sent, size);
                }
            }
        });

        Response r = waitFor(Protocol.SEND_RESULT);
        return new SendResult((boolean) r.payload, r.message);
    }

    public synchronized int pullPendingAndHandle() throws Exception {
        send(w -> w.writeInt(Protocol.PULL_PENDING));

        Response r = waitFor(Protocol.PENDING_RESULT);
        @SuppressWarnings("unchecked")
        List<IncomingFile> list = (List<IncomingFile>) r.payload;

        for (IncomingFile f : list) {
            if (incomingFileListener != null) {
                incomingFileListener.onIncomingFile(f.fileId, f.sender, f.fileName, f.size, f.bytes);
            }
        }
        return list.size();
    }

    public synchronized List<HistoryRow> history(String keyword) throws Exception {
        send(w -> {
            w.writeInt(Protocol.HISTORY);
            w.writeUTF(keyword == null ? "" : keyword);
        });

        Response r = waitFor(Protocol.HISTORY_RESULT);
        @SuppressWarnings("unchecked")
        List<HistoryRow> list = (List<HistoryRow>) r.payload;
        return list;
    }

    // ===== 내부 =====

    private interface Writer {
        void write(DataOutputStream out) throws Exception;
    }

    private void send(Writer writer) throws Exception {
        synchronized (out) {
            writer.write(out);
            out.flush();
        }
    }

    private Response waitFor(int expectedOp) throws Exception {
        while (true) {
            Response r = responses.take();
            if (r.op == expectedOp) return r;
            // Nếu lạc opcode (hiếm), bỏ qua / có thể log
        }
    }

    private void startReaderThread() {
        Thread t = new Thread(() -> {
            try {
                while (socket != null && !socket.isClosed()) {
                    int op = in.readInt();

                    if (op == Protocol.FILE_INCOMING) {
                        int fileId = in.readInt();
                        String sender = in.readUTF();
                        String fileName = in.readUTF();
                        long size = in.readLong();
                        byte[] bytes = readBytes(size);

                        if (incomingFileListener != null) {
                            incomingFileListener.onIncomingFile(fileId, sender, fileName, size, bytes);
                        }
                    }

                    else if (op == Protocol.LOGIN_RESULT) {
                        boolean ok = in.readBoolean();
                        String msg = in.readUTF();
                        responses.put(new Response(op, ok, msg));
                    }

                    else if (op == Protocol.USERS_RESULT) {
                        int n = in.readInt();
                        List<UserOnline> list = new ArrayList<>();
                        for (int i=0;i<n;i++) {
                            String u = in.readUTF();
                            boolean online = in.readBoolean();
                            list.add(new UserOnline(u, online));
                        }
                        responses.put(new Response(op, list, ""));
                    }

                    else if (op == Protocol.SEND_RESULT) {
                        boolean ok = in.readBoolean();
                        String msg = in.readUTF();
                        responses.put(new Response(op, ok, msg));
                    }

                    else if (op == Protocol.PENDING_RESULT) {
                        int n = in.readInt();
                        List<IncomingFile> list = new ArrayList<>();
                        for (int i=0;i<n;i++) {
                            int fileId = in.readInt();
                            String sender = in.readUTF();
                            String fileName = in.readUTF();
                            long size = in.readLong();
                            byte[] bytes = readBytes(size);
                            list.add(new IncomingFile(fileId, sender, fileName, size, bytes));
                        }
                        responses.put(new Response(op, list, ""));
                    }

                    else if (op == Protocol.HISTORY_RESULT) {
                        int n = in.readInt();
                        List<HistoryRow> list = new ArrayList<>();
                        for (int i=0;i<n;i++) {
                            int fileId = in.readInt();
                            String fileName = in.readUTF();
                            long size = in.readLong();
                            String sender = in.readUTF();
                            String receiver = in.readUTF();
                            String status = in.readUTF();
                            String sentAt = in.readUTF();
                            list.add(new HistoryRow(fileId, fileName, size, sender, receiver, status, sentAt));
                        }
                        responses.put(new Response(op, list, ""));
                    }

                    else if (op == Protocol.ERROR) {
                        String msg = in.readUTF();
                        responses.put(new Response(op, false, msg));
                    }

                    else {
                        // opcode không rõ -> bỏ
                    }
                }
            } catch (Exception ignored) {
            }
        }, "client-reader");
        t.setDaemon(true);
        t.start();
    }

    private byte[] readBytes(long size) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int)Math.min(size, 1_000_000));
        byte[] buf = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            int r = in.read(buf, 0, (int)Math.min(buf.length, remaining));
            if (r == -1) throw new EOFException("EOF while reading bytes");
            bos.write(buf, 0, r);
            remaining -= r;
        }
        return bos.toByteArray();
    }

    private static class Response {
        final int op;
        final Object payload;
        final String message;
        Response(int op, Object payload, String message){
            this.op=op; this.payload=payload; this.message=message;
        }
    }

    private static class IncomingFile {
        final int fileId;
        final String sender;
        final String fileName;
        final long size;
        final byte[] bytes;
        IncomingFile(int id, String s, String fn, long z, byte[] b){
            fileId=id; sender=s; fileName=fn; size=z; bytes=b;
        }
    }

    public static class LoginResult {
        public final boolean ok;
        public final String message;
        public LoginResult(boolean ok, String message){ this.ok=ok; this.message=message; }
    }

    public static class SendResult {
        public final boolean ok;
        public final String message;
        public SendResult(boolean ok, String message){ this.ok=ok; this.message=message; }
    }

    public static class UserOnline {
        public final String username;
        public final boolean online;
        public UserOnline(String u, boolean on){ username=u; online=on; }
    }

    public static class HistoryRow {
        public final int fileId;
        public final String fileName;
        public final long size;
        public final String sender;
        public final String receiver;
        public final String status;
        public final String sentAt;
        public HistoryRow(int id, String fn, long s, String se, String re, String st, String at){
            fileId=id; fileName=fn; size=s; sender=se; receiver=re; status=st; sentAt=at;
        }
    }

    public interface ProgressListener {
        void onProgress(long sent, long total);
    }
}