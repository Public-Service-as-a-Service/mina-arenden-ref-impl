package se.psaas.minaarenden.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * En kundhändelse i ärendecachen. Fälten motsvarar objektet kundhandelse i API-specifikationen,
 * kompletterat med den part (kund och/eller ärende) som händelsen gäller.
 */
@Entity
@Table(name = "kundhandelse")
public class KundhandelseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kundhandelse_id", nullable = false, length = 100)
    private String kundhandelseId;

    @Column(nullable = false, length = 200)
    private String producent;

    @Column(name = "kund_identifierare", length = 12)
    private String kundIdentifierare;

    @Column(name = "kund_typ", length = 20)
    private String kundTyp;

    @Column(name = "kund_tillagg", length = 20)
    private String kundTillagg;

    @Column(name = "arende_identifierare", length = 100)
    private String arendeIdentifierare;

    @Column(name = "arende_typ", length = 30)
    private String arendeTyp;

    @Column(nullable = false)
    private String rubrik;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String beskrivning;

    @Column(nullable = false, length = 10)
    private String sprak;

    @Column(nullable = false)
    private Instant tidpunkt;

    @Column(name = "kundhandelse_typ", nullable = false, length = 200)
    private String kundhandelseTyp;

    @Column(name = "producentarendet_kraver_kundatgard", nullable = false)
    private boolean producentarendetKraverKundatgard;

    @Column(name = "producentarendet_klart", nullable = false)
    private boolean producentarendetKlart;

    @Column(nullable = false, length = 10)
    private String version;

    @Column(name = "utokad_information", columnDefinition = "TEXT")
    private String utokadInformation;

    @Column(columnDefinition = "TEXT")
    private String referenser;

    @Column(nullable = false)
    private Instant skapad;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "kundhandelse_tagg", joinColumns = @JoinColumn(name = "kundhandelse_ref"))
    @Column(name = "tagg", nullable = false, length = 100)
    private Set<String> taggar = new LinkedHashSet<>();

    public Long getId() { return id; }
    public String getKundhandelseId() { return kundhandelseId; }
    public void setKundhandelseId(String v) { this.kundhandelseId = v; }
    public String getProducent() { return producent; }
    public void setProducent(String v) { this.producent = v; }
    public String getKundIdentifierare() { return kundIdentifierare; }
    public void setKundIdentifierare(String v) { this.kundIdentifierare = v; }
    public String getKundTyp() { return kundTyp; }
    public void setKundTyp(String v) { this.kundTyp = v; }
    public String getKundTillagg() { return kundTillagg; }
    public void setKundTillagg(String v) { this.kundTillagg = v; }
    public String getArendeIdentifierare() { return arendeIdentifierare; }
    public void setArendeIdentifierare(String v) { this.arendeIdentifierare = v; }
    public String getArendeTyp() { return arendeTyp; }
    public void setArendeTyp(String v) { this.arendeTyp = v; }
    public String getRubrik() { return rubrik; }
    public void setRubrik(String v) { this.rubrik = v; }
    public String getBeskrivning() { return beskrivning; }
    public void setBeskrivning(String v) { this.beskrivning = v; }
    public String getSprak() { return sprak; }
    public void setSprak(String v) { this.sprak = v; }
    public Instant getTidpunkt() { return tidpunkt; }
    public void setTidpunkt(Instant v) { this.tidpunkt = v; }
    public String getKundhandelseTyp() { return kundhandelseTyp; }
    public void setKundhandelseTyp(String v) { this.kundhandelseTyp = v; }
    public boolean isProducentarendetKraverKundatgard() { return producentarendetKraverKundatgard; }
    public void setProducentarendetKraverKundatgard(boolean v) { this.producentarendetKraverKundatgard = v; }
    public boolean isProducentarendetKlart() { return producentarendetKlart; }
    public void setProducentarendetKlart(boolean v) { this.producentarendetKlart = v; }
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public String getUtokadInformation() { return utokadInformation; }
    public void setUtokadInformation(String v) { this.utokadInformation = v; }
    public String getReferenser() { return referenser; }
    public void setReferenser(String v) { this.referenser = v; }
    public Instant getSkapad() { return skapad; }
    public void setSkapad(Instant v) { this.skapad = v; }
    public Set<String> getTaggar() { return taggar; }
    public void setTaggar(Set<String> v) { this.taggar = v == null ? new LinkedHashSet<>() : new LinkedHashSet<>(v); }
}
