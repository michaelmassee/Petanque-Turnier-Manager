package de.petanqueturniermanager.ptmonline.dto;

/** Serverbestätigte exklusive Bindung eines PTM-Dokuments an ein Online-Turnier. */
public record SyncBindingDto(boolean ok, String syncDocumentId, long bindingRevision) {}
