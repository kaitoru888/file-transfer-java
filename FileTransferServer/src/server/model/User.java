package server.model;

public class User {
    public int userId;
    public String username;
    public String passwordHash;
    public String fullName;
    public String status;

    public User(){}
    public User(int id, String u, String fn, String st){
        userId=id; username=u; fullName=fn; status=st;
    }
}