package se.psaas.minaarenden.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import se.psaas.minaarenden.api.dto.Arende;
import se.psaas.minaarenden.api.dto.Kund;
import se.psaas.minaarenden.api.dto.Sortering;
import se.psaas.minaarenden.service.OgiltigFragaException;

/** Databasfilter och sortering som motsvarar fälten i fraga. */
public final class KundhandelseSpecifications {

    private KundhandelseSpecifications() {}

    /** Parten matchar om både angiven kund och angivet ärende stämmer (spec: "svaret uppfyller båda kriterierna"). */
    public static Specification<KundhandelseEntity> forPart(Kund kund, Arende arende) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (kund != null) {
                p.add(cb.equal(root.get("kundIdentifierare"), kund.identifierare()));
                p.add(cb.equal(root.get("kundTyp"), kund.typ()));
                if (kund.tillagg() != null && !kund.tillagg().isBlank()) {
                    p.add(cb.equal(root.get("kundTillagg"), kund.tillagg()));
                }
            }
            if (arende != null) {
                p.add(cb.equal(root.get("arendeIdentifierare"), arende.identifierare()));
                p.add(cb.equal(root.get("arendeTyp"), arende.typ()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** En kundhändelsetyp matchar filtret om den är lika med det eller ligger under det (prefix följt av punkt). */
    public static Specification<KundhandelseEntity> kundhandelseTyper(List<String> typer) {
        if (typer == null || typer.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> cb.or(typer.stream()
                .map(t -> cb.or(cb.equal(root.get("kundhandelseTyp"), t), cb.like(root.get("kundhandelseTyp"), escapeLike(t) + ".%", '!')))
                .toArray(Predicate[]::new));
    }

    /**
     * Minst en av taggarna ska finnas på händelsen. Uttrycks som EXISTS i stället för JOIN + DISTINCT så att
     * sortering på uttryck (t.ex. LOWER(rubrik)) fungerar i alla databaser och inga dubbletter uppstår.
     */
    public static Specification<KundhandelseEntity> taggar(List<String> taggar) {
        if (taggar == null || taggar.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<KundhandelseEntity> inner = sub.from(KundhandelseEntity.class);
            Join<KundhandelseEntity, String> tagg = inner.join("taggar");
            sub.select(inner.get("id")).where(cb.equal(inner.get("id"), root.get("id")), tagg.in(taggar));
            return cb.exists(sub);
        };
    }

    public static Specification<KundhandelseEntity> tidpunktFrom(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("tidpunkt"), from);
    }

    public static Specification<KundhandelseEntity> tidpunktTo(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("tidpunkt"), to);
    }

    /**
     * Sortering enligt fraga.behandling.sortering, utförd i databasen. Utan angiven sortering nyast först.
     * Tidpunkt sorteras på den lagrade DATETIME-kolumnen, inte på RFC 3339-texten, så ordningen blir
     * kronologisk även när UTC-offset skiljer sig (t.ex. vid omställning till vintertid). Primärnyckeln
     * läggs sist som avgörande kriterium så att paginering blir stabil vid lika värden.
     */
    public static Specification<KundhandelseEntity> sorterad(List<Sortering> sortering) {
        return (root, query, cb) -> {
            if (query == null) {
                return null;
            }
            List<Order> orders = new ArrayList<>();
            if (sortering == null || sortering.isEmpty()) {
                orders.add(cb.desc(root.get("tidpunkt")));
                orders.add(cb.desc(root.get("id")));
            } else {
                for (Sortering s : sortering) {
                    Expression<?> uttryck = sorteringsuttryck(root, cb, s.attribut());
                    orders.add(Boolean.TRUE.equals(s.stigande()) ? cb.asc(uttryck) : cb.desc(uttryck));
                }
                orders.add(cb.asc(root.get("id")));
            }
            query.orderBy(orders);
            return null;
        };
    }

    private static Expression<?> sorteringsuttryck(Root<KundhandelseEntity> root, CriteriaBuilder cb, String attribut) {
        return switch (attribut) {
            case "RUBRIK" -> cb.lower(root.get("rubrik"));
            case "BESKRIVNING" -> cb.lower(root.get("beskrivning"));
            case "PRODUCENT" -> cb.lower(root.get("producent"));
            case "TIDPUNKT" -> root.get("tidpunkt");
            case "KUNDHANDELSETYP" -> root.get("kundhandelseTyp");
            case "PRODUCENTARENDETKRAVERKUNDATGARD" -> root.get("producentarendetKraverKundatgard");
            case "PRODUCENTARENDETKLART" -> root.get("producentarendetKlart");
            default -> throw new OgiltigFragaException("Okänt sorteringsattribut: " + attribut);
        };
    }

    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
