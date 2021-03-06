package cz.mikropsoft.mhdwidget.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;
import org.joda.time.LocalTime;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "spoj")
@NamedQueries({
        @NamedQuery(
                name = "Spoj.findFirstByZastavka",
                query = "SELECT s FROM Spoj s WHERE s.zastavka = ?1 AND s.odjezd = (SELECT MIN(odjezd) FROM Spoj WHERE zastavka = s.zastavka)"),
        @NamedQuery(
                name = "Spoj.findLastByZastavka",
                query = "SELECT s FROM Spoj s WHERE s.zastavka = ?1 AND s.odjezd = (SELECT MAX(odjezd) FROM Spoj WHERE zastavka = s.zastavka)"),
})
public class Spoj {

    @Id
    @GeneratedValue
    @Column(unique = true, nullable = false)
    protected Integer id;

    @ManyToOne(targetEntity = Zastavka.class)
    @JoinColumn(name = "zastavka_id", referencedColumnName = "id")
    @JsonBackReference
    private Zastavka zastavka;

    @Column(nullable = false)
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalTime")
    private LocalTime odjezd;

    @OneToOne(targetEntity = Spoj.class)
    @JsonIgnore
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "predchozi_id", referencedColumnName = "id", insertable = false)
    private Spoj predchozi;

    @OneToOne(targetEntity = Spoj.class)
    @JsonIgnore
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "nasledujici_id", referencedColumnName = "id", insertable = false)
    private Spoj nasledujici;

    public Spoj() {
    }

    public Spoj(@NotNull Zastavka zastavka, @NotNull LocalTime odjezd) {
        this.zastavka = zastavka;
        this.odjezd = odjezd;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @NotNull
    public Zastavka getZastavka() {
        return zastavka;
    }

    public void setZastavka(Zastavka zastavka) {
        this.zastavka = zastavka;
    }

    @NotNull
    public LocalTime getOdjezd() {
        return odjezd;
    }

    public void setOdjezd(LocalTime odjezd) {
        this.odjezd = odjezd;
    }

    public Spoj getPredchozi() {
        return predchozi;
    }

    public void setPredchozi(Spoj predchozi) {
        this.predchozi = predchozi;
    }

    public Spoj getNasledujici() {
        return nasledujici;
    }

    public void setNasledujici(Spoj nasledujici) {
        this.nasledujici = nasledujici;
    }

    @Override
    public String toString() {
        return "Spoj: id=" + id;
    }

}
