package de.petanqueturniermanager.webserver;

public record RegieQuelleInfo(String viewId, String anzeigename, int port, boolean laeuft,
        String dokumentName, boolean master) {

    public RegieQuelleInfo(String viewId, String anzeigename, int port, boolean laeuft) {
        this(viewId, anzeigename, port, laeuft, "", true);
    }
}
