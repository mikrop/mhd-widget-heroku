package cz.mikropsoft.mhdwidget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import javax.persistence.*;

@Entity
@Table(name = "linka")
public class Linka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected int id;

    @Column(unique = true)
    private String smer;

    @Column(name = "platnost_do")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDate")
    private LocalDate platnostDo;

    @Column
    @JsonIgnore
    private String url;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSmer() {
        return smer;
    }

    public void setSmer(String smer) {
        this.smer = smer;
    }

    public LocalDate getPlatnostDo() {
        return platnostDo;
    }

    public void setPlatnostDo(LocalDate platnostDo) {
        this.platnostDo = platnostDo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
