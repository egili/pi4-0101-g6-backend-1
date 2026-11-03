package cofre.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "senha_mestra_hash", nullable = false)
    private String senhaMestraHash;

    @Column(nullable = false)
    private String salt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSenhaMestraHash() { return senhaMestraHash; }
    public void setSenhaMestraHash(String senhaMestraHash) { this.senhaMestraHash = senhaMestraHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
}