package cz.mikropsoft.mhdwidget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "zastavka")
public class Zastavka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected int id;

    @ManyToOne(targetEntity = Linka.class)
    @JoinColumn(name = "linka_id")
    private Linka linka;

    @Column(nullable = false)
    private String jmeno;

    @Column
    @JsonIgnore
    private String url;

    @OneToMany(mappedBy = "zastavka", cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, targetEntity = Spoj.class, orphanRemoval = true)
    @JsonManagedReference
    private List<Spoj> spoje = new LinkedList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Linka getLinka() {
        return linka;
    }

    public void setLinka(Linka linka) {
        this.linka = linka;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @NotNull
    public String getJmeno() {
        return jmeno;
    }

    public void setJmeno(String jmeno) {
        this.jmeno = jmeno;
    }

    public List<Spoj> getSpoje() {
        return spoje;
    }

    public void setSpoje(List<Spoj> spoje) {
        this.spoje = spoje;
    }

}
