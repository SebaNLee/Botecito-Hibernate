package ar.edu.itba.paw.models;

public class User {
    private final Long id;
    private final String email;
    private final String password;
    private final String username;

    // TODO, could use Lombok

    public User(Long id, String email, String password, String username) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.username = username;
    }

    public Long getId() {
        return id;
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
