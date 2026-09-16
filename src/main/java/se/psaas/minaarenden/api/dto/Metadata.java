package se.psaas.minaarenden.api.dto;

import java.util.List;

public record Metadata(List<Part> parter, List<String> kundhandelseTyper, List<String> taggar, String startDatum, String slutDatum,
                       String onskatSprak, Behandling behandling) {
}
