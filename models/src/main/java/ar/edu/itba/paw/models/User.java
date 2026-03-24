package ar.edu.itba.paw.models;

public class User {
    private final String email;
    private final String password;
    private final String username;

    // TODO, could use Lombok

    public User(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "User [email=" + email + ", password=" + password + ", username=" + username + "]";
    }
}
