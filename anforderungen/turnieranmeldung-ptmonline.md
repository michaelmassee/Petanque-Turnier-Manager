# Turnieranmeldung mit PTM Online – Soll-Anforderung

**Status:** fachliche und technische Soll-Spezifikation für die Implementierung. Aussagen mit „muss“ sind Abnahmekriterien; „soll“ kennzeichnet eine wichtige, aber nachrangige Anforderung. Diese Seite beschreibt keinen zugesicherten Ist-Stand.

Die Implementierung muss PTM Online als Eingang für öffentliche Anmeldungen und die PTM-Meldeliste als maßgebliche Arbeitsliste am Turniertag verbinden. Anmeldung und Anwesenheit dürfen dabei nie gleichgesetzt werden.

## Geltungsbereich und Datenhoheit

| Bereich | Führendes System | Vorgabe |
| - | - | - |
| Ausschreibung, Anmeldeformular, Status „offen/bestätigt/Warteliste/storniert“ | PTM Online | PTM darf diese Online-Informationen beim Import lesen, aber nicht durch Check-in ersetzen. |
| Lokale Meldeliste, Check-in, Teilnahme, Setzposition, Auslosung und Ergebnisse | PTM | PTM muss diese Daten während der Turnierdurchführung verbindlich führen und an PTM Online übertragen können. |
| Benutzer-ID registrierter Teilnehmer | PTM Online (Vergabe und Gültigkeit) | Wird in PTM pro Person gespeichert. PTM erhält sie in dieser Ausbaustufe nur aus PTM Online und sendet sie bei Übertragungen unverändert zurück; weder Name noch E-Mail ersetzen sie (E-19). |
| E-Mail, Tarife, Antworten auf Online-Fragen und Nachricht der Turnierleitung | PTM Online | Diese Daten bleiben online geführt und dürfen beim PTM-Abgleich nicht überschrieben werden. |
| Namen von Spielern und Teams | PTM Online bis zum Check-in des Teams bzw. bis `running`, danach PTM | Einseitige Änderungen werden abgeglichen; beidseitige Änderungen werden als Konflikt angezeigt (E-16). |
| Teambesetzung eines Formée-Teams | PTM Online in der Anmeldephase, PTM ab Check-in des Teams | Änderungen werden pro Person-Slot abgeglichen; beidseitige Änderungen sind ein Konflikt (E-16, E-20). |
| Technische Zuordnung, Dokumentbindung und Revision | PTM und PTM Online | Diese Daten dienen ausschließlich der Synchronisation und dürfen nicht als normale Meldedaten bearbeitet werden. |


Für eine spätere Ausbaustufe vorgesehen und nicht Teil dieser Anforderung sind Anmeldung und Check-in per Anmelde-Code (Kurzcode oder QR-Code, EW-01) sowie ein Live-Link pro Anmeldung für Teammitglieder ohne Konto (EW-02), siehe Abschnitt „Erweiterungen für einen späteren Ausbau“.

Nicht Teil dieser Anforderung sind automatische Teamentscheidungen bei mehrdeutigen Namen, ein automatischer Check-in durch die Online-Anmeldung oder eine automatische Übernahme offener beziehungsweise stornierter Anmeldungen.

## Geklärte Produktentscheidungen

Diese Entscheidungen schließen die fachlichen Fragen für die Implementierung. Jede Regel ist zusätzlich durch einen Abnahmetest abzusichern.

| Nr. | Frage | Verbindliche Entscheidung |
| - | - | - |
| E-01 | Private oder öffentliche Turniere | Ein **öffentliches** Turnier ist in Suche und Listen sichtbar und über seine normale URL erreichbar. Ein **privates** Turnier ist nicht suchbar und nicht gelistet. Es darf jedoch über einen widerrufbaren privaten Freigabelink erreichbar sein. Beide Arten dürfen Online-Anmeldungen zulassen. Ein Entwurf ist weder öffentlich noch über einen Freigabelink anmeldbar. |
| E-02 | Ab wann ist eine Online-Anmeldung möglich? | Erst wenn das Turnier als öffentlich oder privat freigegeben ist, die Anmeldung geöffnet und der Turnierstatus noch nicht `running` ist. Unabhängig von PTM schließt PTM Online die Anmeldung spätestens zum angesetzten Turnierbeginn (Datum und Startzeit) automatisch, damit sie auch bei einem Turnier ohne Netz nicht offen bleibt. Bei privaten Turnieren ist zusätzlich ein gültiger Freigabelink nötig. Das Anmeldeformular muss bei geschlossener Anmeldung, ungültigem Link oder laufendem Turnier serverseitig ablehnen und die Ursache anzeigen. |
| E-03 | Ab wann darf ein PTM-Dokument verbinden? | Sobald ein passendes Online-Turnier existiert, das API-Key-Konto es verwalten darf und die lokale PTM-Datei dasselbe Turniersystem sowie dieselbe Anmeldeart verwendet. Die Verbindung muss auch vor der Veröffentlichung möglich sein, damit der Veranstalter testen kann. Nach dem Turnierstart ist eine neue Verbindung nur als explizite Wiederherstellung mit Warnung zulässig, niemals stillschweigend. |
| E-04 | Ab wann sind lokale/offline Meldungen erlaubt? | Die Turnierleitung darf lokale Meldungen unmittelbar nach dem Erzeugen der PTM-Meldeliste erfassen, auch ohne Netz und auch vor der Verbindung. Ein Abgleich darf erst nach erfolgreicher Verbindung ausgeführt werden. Vor dem ersten Rundenstart muss PTM einen letzten Abgleich anbieten; bei fehlendem Netz muss die Turnierleitung die Abweichung ausdrücklich bestätigen, bevor sie fortfährt. |
| E-05 | Was geschieht beim Turnierstart? | Der erste Rundenstart ist die fachliche Grenze: PTM prüft fehlende bestätigte Online-Meldungen, setzt das Online-Turnier als ersten Schreibvorgang atomar auf `running` und überträgt danach lokalen Check-in und Setzpositionen (KP-05). Die Auslosung darf durch Netzfehler nicht blockieren. Sie darf aber erst nach einer sichtbaren Entscheidung der Turnierleitung erfolgen, wenn der Vorabcheck nicht vollständig war. Vor dem ersten Übergang zu `running` warnt PTM ausdrücklich, dass damit die Online-Anmeldung geschlossen wird (E-23). |
| E-06 | Doppelte oder dreifache Erfassung | Eine Anmeldung wird durch stabile IDs, nicht durch Name oder E-Mail, wiedererkannt. Für registrierte Teilnehmer ist die Benutzer-ID der Personenschlüssel, online wie in PTM (E-19). Eine identische Online- und lokale Meldung muss verknüpft statt erneut angelegt werden. Meldet sich eine Person mehrfach an oder ist sie in mehreren Teams, darf PTM keine automatische Auswahl treffen. Ist die Identität über dieselbe Benutzer-ID sicher, sind die betroffenen Meldungen als Konflikt zu markieren und von der Auslosung auszuschließen, bis die Turnierleitung sie auflöst. Beruht der Verdacht nur auf Namen oder E-Mail (Gäste), wird er als Hinweis ohne automatische Wirkung angezeigt (KP-06). |
| E-07 | Online-Turnier wird gelöscht | PTM Online muss das Löschen eines verbundenen Turniers bestätigen lassen und die Folgen nennen. Nach dem Löschen muss PTM die Verbindung als beendet markieren, das Sync-Blatt als Archiv erhalten und das lokale Turnier ohne Datenverlust weiterführen. Weitere Online-Schreibvorgänge sind zu sperren; ein neues, passendes Online-Turnier kann anschließend bewusst verbunden werden. |
| E-08 | PTM-Dokument wird gelöscht oder ist verloren | PTM Online darf die Bindung nicht automatisch freigeben, da es das lokale Löschen nicht erkennen kann. Ein neues Dokument muss über einen expliziten Wiederherstellungsablauf verbinden: Online-Turnier auswählen, bestehende Bindung sehen, Übernahme ausdrücklich bestätigen. Die Übernahme muss die alte Schreibberechtigung atomar ungültig machen. Anschließend werden bestätigte Online-Meldungen importiert. Rein lokale, nie abgeglichene Daten sind nicht wiederherstellbar und müssen vor der Übernahme deutlich als Verlusthinweis erscheinen. |
| E-09 | Check-in und Benachrichtigung | Check-in wird ausschließlich von der Turnierleitung in PTM gesetzt; eine Online-Anmeldung setzt ihn nie. Nach erfolgreichem Check-in muss die Live-Ansicht der Anmeldung (E-21) für alle berechtigten Konten den Status ohne Seitenwechsel aktualisieren. Eine E-Mail wird beim Check-in **nicht** versandt, um Vor-Ort-Check-ins nicht als Mailflut auszulösen. Eine Push-/Postbox-Benachrichtigung „Eingecheckt“ wird standardmäßig an alle Konten versandt, die mit einem Personen-Slot der Anmeldung verknüpft sind (E-22), genau einmal pro Anmeldung und Konto. Sie geht an das verknüpfte Konto (Benutzer-ID), nicht an die Kontakt-E-Mail der Anmeldung. Der Veranstalter kann sie pro Turnier abschalten. Der bisherige Live-Link per E-Mail beim Check-in entfällt ersatzlos; ein Live-Link ist erst mit EW-02 wieder vorgesehen. |
| E-10 | Teilnehmer ändert E-Mail im Benutzerprofil | Die Zugehörigkeit zu einer Anmeldung und die Berechtigung für „Meine Live-Turniere“ müssen an der stabilen Benutzer-ID hängen und durch eine E-Mail-Änderung erhalten bleiben. Die in der Anmeldung gespeicherte Kontaktadresse bleibt als historischer Kontakt erhalten; konto-bezogene Benachrichtigungen gehen an die aktuelle, verifizierte Profiladresse. |
| E-11 | Turnierleitung korrigiert eine E-Mail | Es gibt zwei Arten von E-Mail an einer Anmeldung: die **Kontakt-E-Mail** der Anmeldung (für Anmeldebestätigung und organisatorische Rückfragen, typischerweise die Adresse des absendenden Kontos) und die optionale **Slot-E-Mail** jeder Person (E-22). Die Kontakt-E-Mail verknüpft nie ein Konto. Eine Korrektur ändert nur die betreffende Adresse dieser Anmeldung und nie ein Benutzerprofil. Wird eine Slot-E-Mail korrigiert und ist der Slot noch unverknüpft, gilt E-22: Gehört die Adresse zu genau einem verifizierten Konto, wird der Slot verknüpft; bei Mehrdeutigkeit ist eine manuelle Klärung erforderlich. Ein bereits verknüpfter Slot bleibt verknüpft (KP-11). Jede Änderung wird protokolliert. |
| E-12 | Anzeige für Teilnehmer nach Check-in | In „Meine Live-Turniere“ und der Live-Detailansicht seiner Anmeldung (E-21) muss der Teilnehmer nach dem Check-in mindestens Turniername, Status „eingecheckt“, zugewiesenes Team beziehungsweise Partner, Teilnahmezustand und – sobald ausgelost – Runde, Gegner und Bahn sehen. Persönliche Kontodaten anderer Teilnehmer dürfen nie sichtbar werden. Vor Check-in zeigt die Ansicht klar „noch nicht eingecheckt“. |
| E-13 | Anmeldungen nach Turnierstart | Sobald der Status `running` gesetzt ist, muss PTM Online jede neue öffentliche, private oder API-basierte Anmeldung serverseitig ablehnen. Die Anmeldeseite zeigt „Anmeldung geschlossen – Turnier läuft“. Nachmeldungen sind danach nur lokal in PTM durch die Turnierleitung möglich; sie werden als lokale Turnierentscheidung behandelt und dürfen keine neue öffentliche Online-Anmeldung erzeugen. |
| E-14 | Online-Storno oder Warteliste nach dem Import | Der Abgleich löscht die lokale Meldung nicht. Er markiert sie als „online storniert“ bzw. „online Warteliste“ und schließt sie von der Auslosung aus, bis die Turnierleitung sie lokal entfernt oder bewusst behält (KP-14). |
| E-15 | Verknüpfte Meldung wird in PTM gelöscht | Keine automatische Online-Stornierung. PTM fragt „online stornieren“ oder „nur lokal entfernen“. Eine nur lokal entfernte Meldung wird nicht erneut importiert (KP-15). |
| E-16 | Name oder Teambesetzung online und lokal geändert | Eine einseitige Änderung wird in ihre Richtung übertragen. Bei beidseitiger, unterschiedlicher Änderung gewinnt keine Seite still: Es entsteht ein Konflikt. Vorausgewählt ist der Online-Stand, solange das Team nicht eingecheckt und das Turnier nicht `running` ist, danach der lokale Stand (KP-16, KP-20). |
| E-17 | Gültigkeit des Schreib-Lease | Kein zeitlicher Ablauf. Der Lease endet nur durch kontrollierte Trennung, bestätigte Übernahme, ausdrückliches Lösen durch den Veranstalter oder Löschung des Online-Turniers (KP-17). |
| E-18 | Mêlée-Teambildung | Die Teambildung ist die in PTM **bereits vorhandene** Mêlée-Übernahme: ein eigener, separater Schritt kurz vor dem Turnierstart, nach Abschluss des Check-ins in der Mêlée-Anmeldeliste und vor dem Start der ersten Runde. Sie mischt nur eingecheckte, noch nicht übernommene Spieler zu Teams in die Meldeliste; übernommene Zeilen werden nie ein zweites Mal übernommen, gebildete Teams nie neu gemischt. Diese Anforderung ändert die Funktion nicht, sondern legt nur ihr Zusammenspiel mit PTM Online fest: Sie ist weder Teil des Abgleichs noch des Rundenstarts; online bleiben die Anmeldungen Einzelanmeldungen, die Teamzuordnung wird übertragen und in der Live-Ansicht angezeigt (KP-18). |
| E-19 | Registrierte Teilnehmer | Ist ein Teilnehmer registrierter Benutzer von PTM Online, ist seine Benutzer-ID online **und** in PTM der Schlüssel für diese Person. Sie wird in PTM pro Person gespeichert, beim Import mitgeliefert, bei Übertragungen zurückgesendet und für Dublettenerkennung, Kontoverknüpfung, Live-Ansicht und Benachrichtigung verwendet. In dieser Ausbaustufe gelangt sie ausschließlich über PTM Online nach PTM (Verknüpfung über die Slot-E-Mail nach E-22, auch nach einer Korrektur nach E-11 oder bei später angelegtem Konto nach KP-10); PTM selbst erfasst keine Benutzer-IDs. Die Erfassung in PTM per Anmelde- oder Spieler-Code ist eine Erweiterung für später (EW-01). Name und E-Mail dienen nur noch bei Gästen ohne Konto als unsicherer Hinweis (KP-19). |
| E-20 | Was ist eine Anmeldung? | Die Anmeldeeinheit hängt von System und Anmeldeart ab. **Genau eine Person** bei Supermêlée, bei Mêlée-Anmeldung (Teams entstehen erst in PTM) und bei der Formation Tête-à-tête. **Team mit 2 Personen bis zur Formationsstärke** bei allen anderen Systemen und Formationen (Formée): Doublette 2, Triplette 2 oder 3. Ersatzspieler gibt es nicht; die Obergrenze ist immer die Formationsstärke. Ein Formée-Team mit weniger Personen, als die Formation verlangt (z. B. Triplette mit 2), ist zulässig und wird normal importiert; vollständig muss es erst zur Auslosung sein. Die Besetzung eines Formée-Teams darf sich während der Anmeldephase (online manuell ausschließlich durch Turnierersteller, Mitverwalter oder Admin, lokal durch die Turnierleitung mit Übertragung durch das verbundene PTM-Dokument; Teammitglieder dürfen die Besetzung nicht ändern) und beim Check-in (nur in PTM) ändern; das Team behält dabei UUID und Online-ID (KP-20). |
| E-21 | Woran hängt die Live-Ansicht? | Die Live-Ansicht hängt an der **Online-Anmeldungs-ID**. Alle Personen einer Anmeldung sehen dieselbe Live-Ansicht mit demselben Inhalt (Status, Team, Runde, Gegner, Bahn). Die Benutzer-ID entscheidet nur, **wer** sie sehen darf: ausschließlich die Konten, die mit einem Personen-Slot dieser Anmeldung verknüpft sind (E-22). Wer die Anmeldung abgeschickt hat, spielt dafür keine Rolle; Veranstalter und Turnierleitung sehen das Turnier über ihre Verwaltungsansicht. Bei Einzelanmeldungen (Mêlée, Supermêlée, Tête-à-tête) hängt sie an der Einzelanmeldung und zeigt ab der Teambildung das zugeordnete Team; alle Personen desselben Mêlée-Teams sehen so denselben Team- und Rundeninhalt. Wird eine Person aus der Anmeldung entfernt, verliert ihr Konto den Zugriff; kommt eine hinzu, erhält ihr Konto ihn (KP-12). Teammitglieder ohne Benutzerkonto haben in dieser Ausbaustufe keinen Zugriff; ein Live-Link pro Anmeldung ist als TODO für später vermerkt (EW-02). |
| E-22 | Wie wird ein Teammitglied mit seinem Konto verknüpft? | Wer eine Anmeldung abschickt, spielt keine Rolle: Das kann ein Teammitglied sein, aber auch ein Vereinsvertreter, der Turnierersteller oder ein Admin. Das absendende Konto erhält dadurch weder einen Slot noch Sonderrechte noch Zugriff auf die Live-Ansicht. Zur Bequemlichkeit ist die Slot-E-Mail der ersten Person mit der Adresse des absendenden Kontos vorbelegt; wer für andere anmeldet, leert oder ändert sie bewusst (Entscheidung: Vorbelegung bleibt). Jeder Personen-Slot wird mit Name und optionaler E-Mail erfasst. Gehört die E-Mail eines Slots zu genau einem verifizierten Konto, wird der Slot automatisch verknüpft, ohne weitere Bestätigung; wird ein Konto mit dieser E-Mail später angelegt, erfolgt die Verknüpfung nach der Verifikation (KP-10). Ein so verknüpftes Konto kann sich unter „Meine Live-Turniere“ mit „Das bin ich nicht“ selbst aus dem Slot lösen; Name und Anmeldung bleiben erhalten, der Vorgang wird protokolliert. Schutz vor Missbrauch: Das Formular und die Bestätigung zeigen dem absendenden Konto nie an, ob eine Slot-E-Mail zu einem Konto gehört oder verknüpft wurde. Jedes automatisch verknüpfte Konto erhält eine Postbox-Nachricht „Du wurdest für Turnier … eingetragen“ mit dem Weg zu „Das bin ich nicht“. Ist ein Konto dadurch in zwei aktiven Anmeldungen, wird keine Anmeldung abgelehnt, sondern beide werden als Konflikt markiert (KP-06 b). Die Slot-E-Mail ist Kontaktdatum und wird online geführt (Datenhoheit). |
| E-23 | Kann `running` zurückgenommen werden? | PTM setzt `running` nie automatisch zurück, auch nicht beim Löschen der ersten Runde. Turnierersteller, Mitverwalter oder Admin dürfen `running` in PTM Online ausdrücklich zurücksetzen, solange online noch kein Rundenergebnis vorliegt, z. B. nach einem Testlauf vor der Veröffentlichung. Das Zurücksetzen öffnet die Anmeldung nicht automatisch wieder, wird protokolliert und dem verbundenen Dokument beim nächsten Abgleich gemeldet. |
| E-24 | Kopien des PTM-Dokuments | Eine Kopie der verbundenen Datei trägt dieselbe Dokument-ID und denselben Lease. Deshalb führt jedes Dokument einen fortlaufenden Schreibzähler, den der Server mitprüft. Schreibt eine Kopie mit einem veralteten Zählerstand, lehnt der Server ab und meldet „Dokument wurde parallel verändert – Kopie im Einsatz?“. Die Turnierleitung klärt, welche Datei gilt; die andere wird über die Übernahme (E-08) ausgeschlossen. |
| E-25 | Supermêlée mit mehreren Spieltagen | Es bleibt **eine** Meldeliste: Sie ist der Spielerpool aller Spieltage, jeder Spieler behält seine Spieler-Nr, und Spieltag- sowie Endrangliste werten über diese Nummer. Jeder Spieltag ist online ein eigenes Turnier mit eigenem Sync-Blatt; ein neuer Spieltag trennt die bisherigen Verbindungen und archiviert deren Sync-Blätter. Der Abgleich ist auf den verbundenen Spieltag beschränkt: Eine Online-Anmeldung mit derselben Besetzung (alle Namen der Meldung, Reihenfolge egal) wie eine vorhandene Zeile wird ohne Rückfrage mit dieser Zeile verknüpft (die Meldeliste lässt keine doppelten Namen zu, Ausnahme zu KP-06 a2); bei Supermêlée ist eine Meldung nach A-28 genau eine Person. Online angelegt werden nur Meldungen mit einem Eintrag in der Spalte des aktiven Spieltags; der übrige Pool bleibt rein lokal. |


### Zustandsmodell

```
Entwurf ── freigeben ──> öffentlich / privat ── Anmeldung öffnen ──> Anmeldung aktiv    
                                                                |    
                                  Anmeldung schließen <─────────+    
                                                                |    
PTM: Check-in und Vorabprüfung ── erste Runde ──> running ──> abgeschlossen    
                                                    |    
                                      keine neuen Online-Anmeldungen
```

`privat` beschreibt die Sichtbarkeit, nicht die Möglichkeit zur Anmeldung. `running` sperrt dagegen ausnahmslos jede neue Online-Anmeldung.

Ergänzende Übergänge:

- `running` wird nie automatisch zurückgenommen. Wird die erste Runde in PTM gelöscht, bleibt das Online-Turnier `running`. Nur Turnierersteller, Mitverwalter oder Admin können `running` ausdrücklich zurücksetzen, solange online noch kein Rundenergebnis vorliegt (E-23); die Anmeldung öffnet sich dadurch nicht automatisch wieder.

- `abgeschlossen` ist ein Endzustand. Ein abgeschlossenes Turnier ist nicht anmeldbar und nicht neu verbindbar, auch nicht über eine Wiederherstellung.

- PTM Online schließt ein laufendes Turnier 48 Stunden nach dem angesetzten Beginn automatisch ab, auch wenn es mit einem Dokument verbunden ist.

- Die Rückkehr von `öffentlich/privat` zu `Entwurf` ist nur ohne bestehende Anmeldungen zulässig.

## Identitäten und Mapping-Tabelle

Es gibt drei Schlüssel mit klar getrennten Aufgaben:

| Schlüssel | Vergeben durch | Zeitpunkt | Identifiziert | Lebensdauer |
| - | - | - | - | - |
| **Online-Anmeldungs-ID** | PTM Online | im Moment der Online-Anmeldung (schon im Status „offen“, vor der Bestätigung) bzw. bei der Online-Anlage einer lokalen Meldung durch den Abgleich | die Anmeldung, also Team oder Einzelperson, systemübergreifend | unveränderlich, auch bei Namens- oder Besetzungsänderungen, Storno und Wiederbestätigung; unabhängig von Benutzer-IDs – auch eine Anmeldung ohne Konto (Gast) hat sie, und das Verknüpfen oder Lösen von Konten ändert sie nicht |
| **Lokale UUID** | PTM | sofort beim Erfassen einer lokalen Meldung (auch offline, auch vor der Verbindung) bzw. beim Import einer Online-Anmeldung | die Meldung im PTM-Dokument | unveränderlich innerhalb des Dokuments; ein neues Dokument (KP-08) vergibt neue UUIDs |
| **Benutzer-ID** | PTM Online | bei der Registrierung eines Kontos | eine registrierte Person | unveränderlich, auch bei E-Mail-Änderung |

Die Online-Anmeldungs-ID ist der systemübergreifende Schlüssel der Meldung und trägt die Live-Ansicht (E-21). Die lokale UUID existiert, damit eine Meldung schon vor der ersten Online-Anlage eindeutig ist. Sie dient beim Anlegen online als **Idempotenzschlüssel**: Der Server speichert sie pro Turnier eindeutig zur neuen Anmeldung. Ein wiederholter Anlageversuch mit derselben UUID liefert die bestehende Online-Anmeldungs-ID zurück, statt eine zweite Anmeldung zu erzeugen (A-13).

**Wie die Zuordnung entsteht:**

- **Online-Anmeldung → PTM:** Beim Import vergibt PTM eine UUID und schreibt sofort die Zeile UUID ↔ Online-Anmeldungs-ID.
- **Lokale Meldung → PTM Online:** PTM vergibt die UUID beim Erfassen; die Spalte Online-Anmeldungs-ID bleibt leer. Der Abgleich sendet die UUID mit, erhält die Online-Anmeldungs-ID und trägt sie ein.
- **Mêlée-Übernahme:** Jede Einzelanmeldung hat ihre eigene Zeile (UUID ↔ Online-Anmeldungs-ID). Das daraus gebildete Team in der Meldeliste erhält eine eigene lokale Team-UUID ohne Online-Anmeldung. Die Mapping-Tabelle hält zusätzlich die Teamzuordnung Team-UUID → Online-Anmeldungs-IDs der Einzelanmeldungen; diese Teamzuordnung wird übertragen (KP-18).

**Inhalt der Mapping-Tabelle im Blatt `PTMOnline Sync`:**

| Ebene | Felder |
| - | - |
| Dokument | Online-Turnier-ID, Dokument-ID, Lease-Token, Schreibzähler (E-24), letzter erfolgreicher Sync-Zeitpunkt, Pause-Status, Puffer ausstehender Übertragungen (T-09), ausstehender `running`-Übergang (KP-05) |
| Meldung (eine Zeile pro Meldung) | lokale UUID; Online-Anmeldungs-ID (leer bis zur Online-Anlage); Herkunft (online/lokal); Anmeldeeinheit (Einzel/Team); zuletzt abgeglichener Online-Status; zuletzt abgeglichener Name; Revision für den Check-in-Push; Vermerke („lokal entfernt“, „online storniert“, „online Warteliste“, „nach Turnierstart eingegangen“, „Konflikt“, „unvollständig“); ausstehende Aufträge (z. B. „online stornieren“) |
| Person (eine Zeile pro Personen-Slot einer Meldung) | lokale UUID der Meldung, Slot-Nummer, zuletzt abgeglichener Name, Benutzer-ID (leer bei Gästen und lokal erfassten Personen, E-19) |
| Mêlée-Teamzuordnung | Team-UUID der Meldeliste → Liste der Online-Anmeldungs-IDs der Einzelanmeldungen |

Nicht in der Mapping-Tabelle stehen Kontakt- und Slot-E-Mails, Tarif und Antworten auf Online-Fragen (Datenhoheit PTM Online). Die Tabelle ist technisch, nicht frei editierbar und wird nicht als Erfassungsblatt verwendet (A-12, T-15, T-17, T-18). Weil der Blattschutz in LibreOffice umgangen werden kann, muss PTM die Zuordnung UUID ↔ Online-Anmeldungs-ID aus dem Server wiederherstellen können: Der Server kennt die UUIDs als Idempotenzschlüssel (T-21).

### Auftragsmatrix der Sync-Schreibaufrufe

| Operation | Auftrags-ID | Schreibzähler | Lease/Dokument |
| - | - | - | - |
| Erstverbindung | stabile Verbindungs-Auftrags-ID, vor dem Senden im Sync-Blatt gespeichert | Ausnahme: Es gibt noch keinen Zähler; eine neue Bindung setzt ihn auf 0, eine Wiederholung liefert den aktuellen Stand | wird erzeugt |
| Übernahme | Übernahme-Auftrags-ID | Ausnahme: setzt den Zähler auf 0 | wird ersetzt |
| Trennen, Start, Anlage/Änderung einer Anmeldung, Teilnahme/Setzposition, Runde, Rangliste, Eckdaten, Mêlée-Teamzuordnung, Online-Storno einer lokal gelöschten Meldung, Entscheidungsprotokoll, Schließen der Online-Anmeldung | ja | ja | ja |
| Lesende Aufrufe (Turnierliste, Anmeldungen, Zuordnung) | nein | nein | beim Wiederherstellen der Zuordnung lesend geprüft |

**Kompatibilitätsspiegel:** Online sind die Personen-Slots führend. Die bisherigen festen Personen- und Benutzer-ID-Felder einer Anmeldung (Spieler, Partner, Partner 2) werden als Spiegel der Slots 1–3 in derselben Transaktion mitgeschrieben, bis sie in einem späteren Projekt abgelöst werden.

## Kritische Pfade

Jeder kritische Pfad beschreibt einen End-to-End-Ablauf zu einer Produktentscheidung (E-xx). Aufgeführt sind die Stellen, an denen Daten verloren gehen, doppelt entstehen oder falsch zugeordnet werden können, sowie das verbindliche Verhalten dort. „Bruchstelle“ bezeichnet den Punkt, an dem der Pfad durch Netz, Nebenläufigkeit oder Bedienfehler abweichen kann. Jede Bruchstelle braucht einen Prüffall.

### KP-01 Sichtbarkeit öffentlich/privat (E-01, A-17, T-11)

**Ablauf:** Entwurf anlegen → als öffentlich oder privat freigeben → bei privat Freigabelink erzeugen und verteilen → Anmeldung öffnen.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Freigabelink wird widerrufen, während eine Person das Formular geöffnet hat | Das Absenden wird serverseitig abgelehnt („Link nicht mehr gültig“). Es entsteht keine Anmeldung. Bestehende Anmeldungen über den alten Link bleiben gültig. | P-19 |
| Privates Turnier wird öffentlich | Das Turnier wird ab sofort gelistet. Ein bestehender Freigabelink bleibt bis zum Widerruf gültig. | P-20 |
| Öffentliches Turnier wird privat | Es verschwindet aus Suche und Listen. Bestehende Anmeldungen und die Live-Ansicht der angemeldeten Konten bleiben erhalten. Neue Anmeldungen sind nur noch über einen Freigabelink möglich. | P-20 |
| Freigabelink eines Entwurfs | Der Link darf die Vorschau zeigen, aber keine Anmeldung annehmen (E-01). | P-21 |
| Rückkehr zu Entwurf mit bestehenden Anmeldungen | Der Server lehnt ab und nennt die Anzahl der Anmeldungen. | P-21 |


### KP-02 Beginn der Online-Anmeldung (E-02)

Dieser Pfad gilt für Anmeldungen über das öffentliche Formular, den Freigabelink, manuelle Anlage in der Weboberfläche und die öffentliche API. Die Online-Anlage lokaler Meldungen durch das verbundene PTM-Dokument ist ein eigener Zugangsweg mit eigenen Regeln (KP-04, T-24).

**Ablauf der Serverprüfung beim Absenden, in dieser Reihenfolge; die erste fehlschlagende Bedingung bestimmt die Meldung:**

1. Turnier existiert und ist nicht gelöscht.

2. Status ist nicht `draft`, `running` oder `abgeschlossen`.

3. Zugriff: Bei einem öffentlichen Turnier genügt die normale URL, bei einem privaten ist ein gültiger, nicht widerrufener Freigabelink nötig.

4. Die Anmeldung ist geöffnet, ein gegebenenfalls gesetzter Anmeldeschluss ist nicht überschritten, und der angesetzte Turnierbeginn ist noch nicht erreicht (automatischer Anmeldeschluss, E-02).

5. Die Anmeldeeinheit stimmt (E-20): genau eine Person bei Supermêlée, Mêlée und Tête-à-tête, sonst 2 Personen bis zur Formationsstärke.

6. Doppelbelegung: Steht eine Benutzer-ID eines Personen-Slots bereits in einer anderen aktiven Anmeldung dieses Turniers, wird die Anmeldung trotzdem angenommen, aber beide Anmeldungen werden als Konflikt markiert (KP-06 b). Das absendende Konto erfährt davon nichts (E-22). Das absendende Konto selbst zählt nur, wenn es in einem Slot steht; ein Konto darf beliebig viele Teams anmelden.

7. Die Kapazität ist nicht erschöpft; andernfalls kommt die Anmeldung auf die Warteliste, falls aktiviert, sonst wird sie abgelehnt.

Alle Prüfungen und das Anlegen der Anmeldung laufen in einer atomaren Operation (T-12). Eine clientseitige Prüfung ist nur Komfort.

### KP-03 Verbinden eines Dokuments (E-03, A-01, A-02, T-01)

| Online-Zustand | Verbinden |
| - | - |
| Entwurf, öffentlich, privat, Anmeldung offen oder geschlossen | zulässig, wenn System und Anmeldeart passen und das Konto berechtigt ist |
| bereits mit einem anderen Dokument verbunden | nur über die Übernahme nach KP-08 |
| `running` | nur als ausdrückliche Wiederherstellung mit Warnung (KP-08) |
| abgeschlossen | nicht zulässig; das Turnier erscheint nicht in der Auswahlliste |


**Bruchstellen:** Zwei Dokumente verbinden gleichzeitig (P-08). Die Verbindung gelingt serverseitig, aber die Antwort geht verloren: Der erneute Versuch desselben Dokuments muss als dieselbe Verbindung erkannt werden (gleiche Dokument-ID) und darf nicht als Fremdbindung gelten (P-22).

### KP-04 Offline-Meldungen und Abgleich (E-04, A-05, A-13)

**Ablauf:** Meldeliste erzeugen → lokale Meldungen erfassen (jede erhält sofort eine UUID) → verbinden → erster Abgleich: Online-Import und Anlage der lokalen Meldungen → weitere Abgleiche → letzter Abgleich vor dem ersten Rundenstart.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Lokale Meldungen vor der Verbindung | Sie bleiben lokal und werden erst beim ersten Abgleich nach der Verbindung online angelegt. Die Verbindung selbst importiert und exportiert nichts. | P-23 |
| Abbruch mitten im Abgleich (Netz, LibreOffice beendet) | Bereits angelegte Online-Anmeldungen werden beim Wiederholen über die UUID als Idempotenzschlüssel wiedererkannt. Der letzte Sync-Zeitpunkt bleibt unverändert (T-08). | P-06 |
| Online-Kapazität ist erschöpft oder Anmeldeschluss erreicht, lokal wird nachgemeldet | Eine lokale Meldung der Turnierleitung ist eine bewusste Entscheidung. Über den lease-gebundenen PTM-Zugangsweg (T-24), nicht über KP-02, wird sie online als bestätigt angelegt, auch über die Kapazität hinaus und nach Anmeldeschluss, solange das Turnier nicht `running` ist. Der Server kennzeichnet sie atomar mit `over_capacity` und protokolliert das; und in der Zusammenfassung als „über Kapazität“ ausgewiesen. In der öffentlichen Teilnehmerliste erscheint sie als „Nachmeldung der Turnierleitung“, damit Wartende die Abweichung nachvollziehen können. | P-24 |
| Letzter Abgleich ohne Netz | PTM zeigt, wann zuletzt erfolgreich abgeglichen wurde, und verlangt eine ausdrückliche Bestätigung. Die Bestätigung wird im Sync-Blatt protokolliert. | P-25 |


### KP-05 Turnierstart (E-05, E-13, A-08, A-18, T-12)

**Ablauf:**

1. Die Turnierleitung wählt „Erste Runde starten“.

2. Vorabcheck, nur lesend, angezeigt in der gesammelten Konfliktliste (A-29): Fehlen bestätigte Online-Anmeldungen lokal? Gibt es offene Konflikte (KP-06)? Bei Mêlée-Anmeldung: Wurde der Schritt „Mêlée-Übernahme“ ausgeführt, und gibt es eingecheckte Personen ohne Team (KP-18)?

3. Bei Abweichung: sichtbare Entscheidung „Abbrechen und passende Aktion ausführen“ (abgleichen, Konflikt auflösen oder Mêlée-Übernahme) oder „Trotzdem starten“.

4. Die Runde wird lokal ausgelost. Das geschieht sofort und unabhängig vom Netz.

5. Asynchron als **erster** Schreibvorgang: Online-Status atomar auf `running` setzen. Das sperrt neue Anmeldungen.

6. Danach Teilnahme, Setzpositionen und Runde übertragen.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Kein Netz beim Start | Das Turnier läuft lokal, online ist die Anmeldung aber noch offen. PTM merkt sich „`running` ausstehend“ und setzt es bei der nächsten Verbindung vor allen anderen Schreibvorgängen. PTM zeigt dauerhaft sichtbar: „Online-Anmeldung noch offen – Turnierstart nicht übertragen“. Spätestens zum angesetzten Turnierbeginn schließt PTM Online die Anmeldung ohnehin selbst (E-02). | P-26, P-65 |
| Online-Anmeldungen zwischen lokalem Start und online gesetztem `running` | Sie werden angenommen, aber mit „nach Turnierstart eingegangen“ markiert, nie automatisch importiert und der Turnierleitung beim nächsten Abgleich gesondert gezeigt. Die Turnierleitung entscheidet: übernehmen (die bestehende Online-Anmeldung wird mit einer lokalen Meldung verknüpft; es entsteht keine neue Online-Anmeldung, E-13) oder stornieren. | P-26 |
| Vorbeugung | PTM bietet beim manuellen Abgleich an, die Online-Anmeldung zu schließen, sobald der Check-in ansteht: Die Anmeldung ist online noch offen, und der Turniertag ist erreicht oder lokal ist bereits jemand eingecheckt. Das Schließen ist ein gezählter Schreibauftrag (`PUT /api/sync/tournaments/{id}/registration-closed`); ohne Netz wird er nachgeholt, wieder geöffnet wird in PTM Online. | P-73 |
| Beim lokalen Start liegen noch nicht gesendete Aufträge aus der Zeit vor dem Start im Puffer | PTM dokumentiert diese Aufträge im Sync-Blatt als „verworfen“ (Auftrags-ID, Zähler, Art, Grund) und sendet sie nie mehr. Danach wird der `running`-Auftrag mit dem nächsten Zähler gespeichert; er ist der einzige Auftrag, der auch während einer Pause gesendet werden darf. Lokale Nachmeldungen, die dadurch nicht online angelegt wurden, bleiben rein lokal (E-13). Den vollständigen Stand von Teilnahme und Setzpositionen überträgt anschließend ein neuer Status-Auftrag mit höherem Zähler. | P-26, P-58 |
| Sync ist beim Start pausiert | Behandlung wie „kein Netz beim Start“: `running` bleibt ausstehend. Der Vorabcheck weist ausdrücklich darauf hin, dass die Online-Anmeldung offen bleibt, und bietet an, trotz Pause nur den Übergang zu `running` zu senden. | P-58 |
| Erste Runde wird lokal gelöscht | `running` bleibt online bestehen, die Anmeldung bleibt geschlossen (siehe Zustandsmodell). | P-27 |
| Versehentlicher Start, z. B. Testlauf vor der Veröffentlichung | PTM hat vor dem ersten `running` gewarnt. Solange online kein Rundenergebnis vorliegt, können Turnierersteller, Mitverwalter oder Admin `running` online zurücksetzen (E-23). | P-64 |
| Lease beim Start ungültig (Übernahme durch ein anderes Dokument) | Die lokale Runde bleibt gültig. Schreibvorgänge werden abgewiesen, und PTM zeigt „Dieses Dokument ist nicht mehr verbunden“ (A-16). | P-15 |


### KP-06 Doppelte und dreifache Erfassung (E-06, A-07)

| Variante | Erkennung | Verbindliches Verhalten | Prüffall |
| - | - | - | - |
| a1) *(greift erst mit Erweiterung EW-01, weil PTM vorher keine Benutzer-IDs lokal erfasst)* Dasselbe Team online **und** lokal erfasst, alle Personen mit Benutzer-ID | Beim Abgleich: Die lokale Meldung ohne Online-ID hat genau dieselbe Menge an Benutzer-IDs wie eine Online-Anmeldung, die noch keiner UUID zugeordnet ist. | Automatisch verknüpfen: Die UUID erhält die Online-ID, es entsteht keine zweite Anmeldung. In der Zusammenfassung als „verknüpft“ ausgewiesen. | P-43 |
| a1') *(Erweiterung EW-01)* Benutzer-IDs überschneiden sich nur teilweise | Mindestens eine gemeinsame Benutzer-ID, aber unterschiedliche Besetzung | Keine automatische Wirkung. Anzeige „möglicherweise dasselbe Team mit geänderter Besetzung“. Die Turnierleitung wählt „verknüpfen“ (die Besetzung wird nach E-16 abgeglichen) oder „getrennt“ (dann gilt c) für die gemeinsamen Personen). | P-48 |
| a2) Dieselbe Person oder dasselbe Team online **und** lokal erfasst, bevor abgeglichen wurde (in dieser Ausbaustufe der Regelfall, weil lokal erfasste Personen keine Benutzer-ID haben) | Beim Abgleich: Die lokale Meldung ohne Online-ID hat dieselben normalisierten Spielernamen wie eine Online-Anmeldung, die noch keiner UUID zugeordnet ist, und keine widersprechende Benutzer-ID. | Weder anlegen noch importieren. Beide werden als „möglicherweise identisch“ gezeigt. Die Turnierleitung wählt entweder „verknüpfen“ (die UUID erhält die Online-ID, die Online-Daten bleiben führend für E-Mail und Tarif) oder „bewusst getrennt“ (danach normale Anlage bzw. normaler Import). Ausnahme Supermêlée-Spielerpool: dort wird ohne Rückfrage verknüpft (E-25). Zusätzlich zeigt PTM ähnliche Namen (Schreibvarianten wie „Müller“/„Mueller“, vertauschte Vor- und Nachnamen) als Hinweis ohne Wirkung an. | P-28 |
| b) Eine Person mit Konto steht in mehreren Anmeldungen (von wem auch immer eingetragen, online oder lokal durch die Turnierleitung) | Serverseitig über die Benutzer-ID in allen Personen-Slots; das absendende Konto zählt nur, wenn es selbst in einem Slot steht | Online: Die zweite Anmeldung wird angenommen; beide Anmeldungen werden als Konflikt markiert, die betroffene Person erhält eine Postbox-Nachricht, und die Turnierleitung löst den Konflikt auf. Keine harte Ablehnung, damit ein Fremder niemanden durch Eintragen aus einem Turnier aussperren kann; das absendende Konto erfährt nichts über die Belegung (E-22). Lokal erfasste Personen haben in dieser Ausbaustufe keine Benutzer-ID; dort gilt nur der Namenshinweis nach c). Mit EW-01 warnt PTM bereits bei der Erfassung, wenn dieselbe Benutzer-ID schon in der Meldeliste steht. | P-29, P-44 |
| b') Mehrfachanmeldung ohne Konto bzw. als Gast | Nur über Namen oder E-Mail, also unsicher | Die Anmeldung wird angenommen und für die Turnierleitung als „mögliche Dublette“ markiert, ohne automatische Wirkung. | P-29 |
| c) Eine Person steht in mehreren Teams (online, lokal oder gemischt) | Gleiche Benutzer-ID in PTM und online; ohne Konto gleicher normalisierter Name (nur Hinweis) | Alle betroffenen Meldungen werden als Konflikt markiert. Check-in ist möglich, aber die Auslosung schließt Konfliktmeldungen aus, bis die Turnierleitung eine davon storniert oder den Konflikt als „verschiedene Personen“ auflöst. | P-30 |


### KP-07 Online-Turnier wird gelöscht (E-07)

**Ablauf:** Löschen in PTM Online → Bestätigungsdialog nennt verbundenes Dokument, Anzahl der Anmeldungen, Turnierstatus und das Ende der Live-Ansicht → Löschen → Server hinterlässt einen Löschnachweis (Tombstone) mit Turnier-ID und Zeitpunkt → PTM erhält beim nächsten Aufruf den eindeutigen Code `tournament_deleted`.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| PTM erhält 404, 403 oder einen Netzfehler | Das gilt **nicht** als Löschung. Nur der ausdrückliche Code `tournament_deleted` beendet die Verbindung. Sonst droht ein unbeabsichtigtes Archivieren. | P-31 |
| Löschung während `running` | Zusätzliche Warnung „Turnier läuft“. Löscht nicht der Turnierersteller selbst, erhält er eine Benachrichtigung (T-22). Das lokale Turnier läuft weiter, Schreibvorgänge werden gesperrt, und das Sync-Blatt wird archiviert. | P-31 |
| Angemeldete Teilnehmer | Ihre Anmeldungen verschwinden; die Live-Ansicht zeigt „Turnier wurde vom Veranstalter gelöscht“ statt eines Fehlers. | P-31 |


### KP-08 Lokales Dokument gelöscht oder verloren – Wiederaufsetzen (E-08, A-19)

**Ablauf:**

1. Neues PTM-Turnier mit gleichem System anlegen und Meldeliste erzeugen.

2. „Mit Online-Turnier verbinden“ → das gebundene Turnier ist mit dem Hinweis „bereits verbunden seit …, letzter Sync …“ sichtbar.

3. „Verbindung übernehmen“ → Verlusthinweis: Alle lokalen, nie abgeglichenen Meldungen und Check-ins nach dem letzten Sync sind verloren.

4. Ausdrückliche Bestätigung → der Server ersetzt Bindung und Lease atomar (A-19). Führt nicht der Turnierersteller die Übernahme aus, erhält er eine Benachrichtigung (T-22).

5. Abgleich → bestätigte Online-Anmeldungen werden importiert, alle ohne Check-in.

6. Die Turnierleitung wiederholt den Check-in.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Die alte Datei taucht wieder auf (Backup, zweiter Laptop) | Alle ihre Schreibvorgänge werden abgewiesen. PTM meldet „nicht mehr verbunden“ und bietet **keine** automatische Rückübernahme an. | P-15 |
| Verlust während `running`, Runden sind bereits online | Die Übernahme warnt zusätzlich: „Online existieren n Runden“. Die neue Datei darf Online-Runden nicht stillschweigend überschreiben oder löschen. Ein Rundenpush, der bestehende Online-Runden ersetzen würde, verlangt eine ausdrückliche Bestätigung. | P-32 |
| Online importierte Meldungen, die lokal Nachmeldungen waren | Sie werden wie Online-Anmeldungen importiert, weil sie online bestätigt sind. | P-15 |


### KP-09 Check-in und Benachrichtigung (E-09, A-20)

**Ablauf:** Die Turnierleitung setzt den Check-in in PTM → die Meldung ist lokal sofort aktiv → asynchroner Push der Teilnahme mit Revision → Server aktualisiert → die Live-Ansicht der Anmeldung zeigt allen berechtigten Konten „eingecheckt“ ohne Neuladen, spätestens 30 Sekunden nach erfolgreichem Push.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| E-Mail | Beim Check-in wird nie eine E-Mail versandt. | P-18 |
| Option „Benachrichtigung bei Check-in“ (Standard: aktiv, pro Turnier abschaltbar) | Genau eine Push- bzw. Postbox-Nachricht pro Anmeldung und Konto, auch bei mehrfachem Ein- und Auschecken (einmaliger Merker). Ziel ist das verknüpfte Konto, nicht die Kontakt-E-Mail der Anmeldung. | P-33 |
| Check-in versehentlich gesetzt und zurückgenommen | Die Live-Ansicht folgt dem aktuellen Stand. Eine bereits versandte Nachricht wird nicht widerrufen, aber auch nicht wiederholt. | P-33 |
| Check-in ohne Netz | Er wird lokal gespeichert und beim Fortsetzen oder beim nächsten Push übertragen (T-09). Die Live-Ansicht zeigt bis dahin „noch nicht eingecheckt“. | P-11 |
| Teilnehmer ohne Konto oder ohne Kontoverknüpfung (auch lokale Nachmeldung, die online noch nicht verknüpft wurde) | Kein Zugriff auf eine Live-Ansicht und keine Nachricht. Die Person erscheint nur in öffentlichen Paarungen und Ranglisten. | P-34 |


### KP-10 Teilnehmer ändert die Profil-E-Mail (E-10, A-21, T-13)

**Ablauf:** Profil-E-Mail ändern → neue Adresse verifizieren → erst danach neue Adresse aktiv. Die Anmeldung hängt weiterhin an der Benutzer-ID.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Die neue Adresse ist noch nicht verifiziert | Kontobezogene Nachrichten gehen weiter an die alte, verifizierte Adresse. | P-16 |
| Person ohne Konto angemeldet, Konto wird später mit der Slot-E-Mail angelegt | Die Verknüpfung dieses Slots entsteht erst nach Verifikation der Kontoadresse und nur bei eindeutiger Übereinstimmung (E-22); sie wird protokolliert. | P-35 |
| Konto wird gelöscht | Die Verknüpfung des Slots entfällt; Name, Slot-E-Mail und Anmeldung bleiben bestehen, die Person gilt danach als Gast. | P-35 |


### KP-11 Turnierleitung korrigiert eine E-Mail (E-11, A-22, T-14)

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Ein Slot ist bereits mit Konto A verknüpft, die korrigierte Slot-E-Mail gehört Konto B (mit Erweiterung EW-01 ebenso, wenn in PTM eine andere Benutzer-ID erfasst wird) | Die Verknüpfung zu A bleibt bestehen. Ein Wechsel auf B ist nur über eine ausdrückliche, protokollierte Aktion „Konto neu zuordnen“ möglich. Sonst könnte eine Tippkorrektur die Live-Ansicht an eine fremde Person übergeben. | P-36 |
| Ein Slot ist unverknüpft, die korrigierte Slot-E-Mail gehört genau einem verifizierten Konto | Der Slot wird verknüpft (E-11, E-22) und das wird protokolliert. Eine Korrektur der Kontakt-E-Mail verknüpft nie. | P-17 |
| Die korrigierte Adresse ist mehrdeutig oder gehört keinem Konto | Keine Verknüpfung, nur die korrigierte Adresse ändert sich. | P-17 |
| Korrektur in PTM statt online | Kontakt- und Slot-E-Mails werden online geführt; PTM überträgt keine E-Mail-Änderungen (Datenhoheit). | P-17 |


### KP-12 Live-Ansicht nach Check-in (E-12, E-21)

Die Ansicht gehört zur Anmeldung, nicht zur einzelnen Person: Alle Personen einer Anmeldung sehen denselben Inhalt. Zugriff haben die mit der Anmeldung verknüpften Konten.

| Phase | Anzeige für den Teilnehmer |
| - | - |
| Angemeldet, nicht eingecheckt | Turniername, Anmeldestatus, „noch nicht eingecheckt“ |
| Eingecheckt, noch nicht ausgelost | „eingecheckt“ und aktuelle Teambesetzung; bei unvollständigem Formée-Team „Team noch unvollständig“; bei Mêlée vor der Teambildung „Team wird kurz vor Turnierstart gebildet“; bei Supermêlée „Team wird bei der Auslosung gebildet“ |
| Ausgelost | Runde, eigenes Team, Mitspieler, Gegner (nur Namen), Bahn |
| Ausgesetzt | „aktuell ausgesetzt“ statt „eingecheckt“ |
| Datenstand älter als der letzte erfolgreiche Push | Zeitstempel „Stand hh:mm“, damit ein Netzausfall in PTM nicht als falsche Auslosung wirkt |


Nie sichtbar sind E-Mail, Telefon, Kontodaten oder Tarif anderer Teilnehmer.

### KP-13 Keine Online-Anmeldung nach Turnierstart (E-13, A-18)

| Zugangsweg | Verhalten ab `running` |
| - | :-: |
| Öffentliches Formular, Freigabelink, API | Serverseitige Ablehnung „Anmeldung geschlossen – Turnier läuft“ |
| Veranstalter legt online manuell eine Anmeldung an | Ebenfalls gesperrt; Nachmeldungen erfolgen ausschließlich in PTM |
| Formular war vor dem Start geöffnet, Absenden danach | Abgelehnt, keine Anmeldung (P-14) |
| Lokale Nachmeldung in PTM nach dem Start | Keine Online-Anmeldung. Sie erscheint online über die Runden- und Ranglistenübertragung. Sie hat in dieser Ausbaustufe keine Benutzer-ID und erscheint nur mit Namen; nach `running` ist auch keine Online-Verknüpfung mehr möglich, weil keine Online-Anmeldung existiert. Eine Live-Ansicht gibt es für sie nicht, weil die Live-Ansicht eine Online-Anmeldungs-ID voraussetzt (E-21). Das gilt auch mit Erweiterung EW-01: Eine dort per Code erfasste Benutzer-ID wird nur in Runden- und Ranglistenübertragung mitgeführt. |
| Warteliste und offene Anmeldungen beim Start | Sie bleiben unverändert und werden nicht automatisch storniert. Die Turnierleitung kann sie nach dem Turnier bereinigen. |


### KP-14 Online-Storno oder Warteliste nach dem Import (E-14)

**Ablauf:** Eine bestätigte Online-Anmeldung wurde importiert → die Turnierleitung storniert sie online oder setzt sie auf die Warteliste → nächster Abgleich.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Die lokale Meldung ist noch nicht eingecheckt | Der Abgleich markiert sie als „online storniert“ bzw. „online Warteliste“. Sie bleibt in der Meldeliste, wird aber nicht gelöscht. | P-37 |
| Die lokale Meldung ist bereits eingecheckt | Gleiche Markierung. Die Meldung wird zusätzlich von der Auslosung ausgeschlossen und in der Abgleich-Zusammenfassung sowie im Vorabcheck vor der nächsten Runde ausdrücklich genannt. | P-37 |
| Die Turnierleitung entscheidet | „Lokal entfernen“ löscht die Meldung. „Bewusst behalten“ hebt den Ausschluss auf. Die Online-Anmeldung bleibt dabei storniert; es entsteht keine neue Online-Anmeldung. Die Entscheidung wird protokolliert. | P-37 |
| Die Anmeldung wird online wieder bestätigt | Die Markierung entfällt beim nächsten Abgleich. Der lokale Check-in-Zustand bleibt unverändert. | P-37 |


### KP-15 Löschen einer verknüpften Meldung in PTM (E-15)

**Ablauf:** Die Turnierleitung löscht in PTM eine Meldung mit Online-ID → PTM fragt sofort nach der Online-Wirkung.

| Wahl | Wirkung | Prüffall |
| - | - | - |
| „Online stornieren“ | Beim nächsten Abgleich wird die Online-Anmeldung auf `storniert` gesetzt. Bis dahin steht der Auftrag im Sync-Blatt. Ohne Verbindung wird er nachgeholt. | P-38 |
| „Nur lokal entfernen“ | Die Online-Anmeldung bleibt unverändert. Sie wird beim nächsten Abgleich **nicht** erneut importiert, weil ihre Online-ID im Sync-Blatt als „lokal entfernt“ vermerkt ist. | P-38 |
| Löschen ohne Dialog (Zeile manuell entfernt) | Der Abgleich erkennt die fehlende UUID. Er storniert nichts und importiert nichts, sondern stellt die Frage aus diesem Dialog erneut. | P-38 |


Ab `running` wird eine Stornierung nur übertragen, wenn die Meldung noch in keiner Runde ausgelost wurde. Sonst wird sie lokal ausgesetzt statt gelöscht.

### KP-16 Namensänderung online und lokal (E-16)

| Situation seit dem letzten Abgleich | Verhalten | Prüffall |
| - | - | - |
| Nur online geändert | Der Name wird lokal übernommen. | P-39 |
| Nur lokal geändert | Der Name wird online übernommen (Änderung des Personen-Slots, A-14). | P-10 |
| Beide Seiten geändert, unterschiedlich | Keine Seite gewinnt still. Der Abgleich zeigt einen Namenskonflikt. Vorausgewählt ist der Online-Name, solange das Team nicht eingecheckt und das Turnier nicht `running` ist, danach der lokale Name (E-16). Die Turnierleitung bestätigt eine Variante. Bis dahin bleiben beide Seiten unverändert, und die Meldung ist nicht gesperrt. | P-39 |
| Beide Seiten geändert, identisch | Kein Konflikt. | P-39 |


Voraussetzung: Das Sync-Blatt speichert pro Meldung den zuletzt abgeglichenen Namen, damit „nur eine Seite geändert“ erkennbar ist (T-15).

### KP-17 Gültigkeit des Schreib-Lease (E-17)

| Situation | Verhalten | Prüffall |
| - | - | - |
| Lange Offline-Phase oder langes Turnier | Der Lease läuft zeitlich nicht ab. Er endet nur durch kontrollierte Trennung, bestätigte Übernahme (KP-08), ausdrückliches Lösen durch den Veranstalter oder Löschung des Online-Turniers (KP-07). | P-40 |
| Anderes Dokument will verbinden | Nur über die Übernahme nach KP-08, nie weil der bestehende Lease „alt“ ist. | P-40 |
| Vergessene Bindung nach dem Turnier | Es gibt keinen automatischen Ablauf. Turnierersteller, Mitverwalter oder Admin können die Bindung in PTM Online ausdrücklich lösen; das beendet den Lease sofort und wird protokolliert. Ist der Turnierersteller nicht mehr erreichbar, löst ein Admin die Bindung. | P-40 |


### KP-18 Mêlée-Teambildung als eigener Schritt vor Turnierstart (E-18)

**Ablauf:** Einzelanmeldungen abgleichen → letzter Abgleich → Check-in in PTM abschließen → **separater Schritt „Mêlée-Übernahme“** → Kontrolle der Teams → Start der ersten Runde (Vorabcheck nach KP-05) → Push der Teamzuordnung.

Die Mêlée-Übernahme ist in PTM bereits vorhanden (Mêlée-Anmeldeliste mit den Spalten „Eingecheckt“ und „Übernommen“). Sie findet bewusst kurz vor dem Turnierstart statt, weil erst dann feststeht, wer anwesend ist, und braucht kein Netz. Neu festgelegt wird hier nur das Verhalten gegenüber PTM Online.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Teambildung im Abgleich, beim Check-in oder beim Rundenstart | Nicht zulässig. Der Abgleich übernimmt nur Einzelanmeldungen (A-10), der Check-in setzt nur die Anwesenheit, und der Rundenstart bildet keine Teams verdeckt. Die Teambildung ist ausschließlich der eigene, ausdrücklich ausgelöste Schritt. | P-41 |
| Rundenstart ohne vorherige Teambildung | Der Vorabcheck hält den Start an und bietet an, den Schritt jetzt auszuführen (KP-05). | P-53 |
| Online-Darstellung gebildeter Teams | Die Anmeldungen bleiben online Einzelanmeldungen und werden weder zusammengelegt noch storniert. Übertragen wird nur die Teamzuordnung; die Live-Ansicht zeigt darüber Team und Mitspieler (E-21). | P-41 |
| Weitere Personen checken ein, nachdem Teams gebildet wurden (Nachzügler) | Der Vorabcheck nennt sie. Ein erneutes „Mêlée-Übernahme“ verwendet nur eingecheckte, noch keinem Team zugeordnete Personen. Bereits gebildete Teams bleiben unverändert und werden nicht neu gemischt. | P-54 |
| Zu wenige offene Personen für ein vollständiges Team | Sie bleiben offen und werden in der Aktion mit Anzahl genannt. Freilos und Ergänzung folgen der Mêlée-Regel des jeweiligen Turniersystems im Hauptprojekt. | P-41 |
| Eine Person aus einem gebildeten Team checkt aus | Das Team wird nicht automatisch aufgelöst. PTM zeigt es als unvollständig; die Turnierleitung entscheidet über Ersatz oder Auflösung. | P-42 |


### KP-19 Registrierte Teilnehmer: Benutzer-ID als Schlüssel online und in PTM (E-19)

Die Meldung (Einzelanmeldung oder Team mit 2 Personen bis zur Formationsstärke, E-20) wird über UUID ↔ Online-Anmeldungs-ID identifiziert (T-03, T-04). Die **Person** wird, sofern sie registriert ist, in beiden Systemen über ihre Benutzer-ID identifiziert. Name und E-Mail sind für registrierte Teilnehmer nie Schlüssel.

**Ablauf in dieser Ausbaustufe:**

1. Online-Anmeldung: Jeder Personen-Slot mit Konto erhält serverseitig die Benutzer-ID nach E-22 über die Slot-E-Mail, unabhängig davon, wer die Anmeldung abgeschickt hat.
2. Import in PTM: Die Benutzer-IDs werden pro Person mitgeliefert und in PTM bei der Meldung gespeichert; beim Sortieren bleiben sie bei der Person.
3. Lokale Erfassung: Die Turnierleitung erfasst Personen in PTM nur mit Namen. PTM vergibt oder sucht keine Benutzer-IDs. Eine Kontoverknüpfung für eine lokal erfasste Meldung entsteht erst online, nachdem der Abgleich sie angelegt hat: Turnierersteller, Mitverwalter oder Admin tragen dort die Slot-E-Mail ein (E-22). Der nächste Abgleich liefert die Benutzer-ID dann an PTM zurück.
4. Check-in, Runden- und Ranglistenübertragung enthalten die Benutzer-IDs; darüber autorisiert die Live-Ansicht (T-13) und werden Benachrichtigungen zugestellt (E-09).

Die Erfassung einer Benutzer-ID direkt in PTM (auch offline) über einen Anmelde- oder Spieler-Code ist ausdrücklich eine **Erweiterung für einen späteren Ausbau** (EW-01).

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Benutzer-ID einer importierten Person wird in PTM verändert | Nicht möglich: Die Benutzer-ID liegt im technischen Sync-Speicher und ist keine frei editierbare Zelle (T-17). Wird die Person in PTM durch eine andere ersetzt (Besetzungsänderung, KP-20), entfällt die Benutzer-ID dieses Slots. | P-45 |
| Lokal erfasste Nachmeldung einer registrierten Person | Wird online ohne Kontoverknüpfung angelegt. Turnierersteller, Mitverwalter oder Admin können in PTM Online die Slot-E-Mail eintragen, wodurch der Slot nach E-22 verknüpft wird; der nächste Abgleich überträgt die Benutzer-ID nach PTM. Vor `running` ist das der Weg zur Live-Ansicht der Anmeldung. | P-47 |
| Konto wird gelöscht | Der Server liefert die Benutzer-ID als ungültig; PTM behält die Person mit Namen, entfernt die Kontoverknüpfung und meldet dies im Abgleich. | P-35 |
| Profil-E-Mail oder Name im Konto ändert sich | Keine Wirkung auf die Zuordnung (E-10). Der Name in der Meldeliste folgt den Regeln aus KP-16, nicht dem Kontonamen. | P-16 |

### KP-20 Teambesetzung ändert sich (E-20)

**Ablauf (nur Formée):** Anmeldung eines Teams mit 2 Personen bis zur Formationsstärke → Änderungen der Besetzung in der Anmeldephase (online oder lokal) → Abgleich → Änderungen beim Check-in (nur PTM) → Push.

| Bruchstelle | Verbindliches Verhalten | Prüffall |
| - | - | - |
| Online-Änderung der Besetzung | Nur durch Turnierersteller, Mitverwalter oder Admin, zulässig bis Anmeldeschluss bzw. `running` (T-18). Teammitglieder können die Besetzung weder online noch über die Live-Ansicht ändern; sie wenden sich an den Veranstalter. Das Team behält Online-ID und UUID; der Abgleich übernimmt die Besetzung lokal, sofern lokal seit dem letzten Abgleich nichts geändert wurde. | P-49 |
| Lokale Änderung (Anmeldephase oder Check-in) | Wird beim nächsten Abgleich bzw. Push online übernommen. Die Übertragung ist durch Dokumentbindung und Schreib-Lease berechtigt (T-18). Neue Personen ohne Konto werden nur mit Namen übertragen. | P-42 |
| Beide Seiten ändern unterschiedlich | Besetzungskonflikt nach E-16; keine Seite überschreibt still. Vorausgewählt ist nach Check-in der lokale Stand. | P-50 |
| Hinzugefügte Person steht bereits in einem anderen Team | Konflikt nach KP-06 c). | P-44 |
| Person wird aus dem Team entfernt | Ihr Konto verliert den Zugriff auf die Live-Ansicht dieser Anmeldung (E-21); es geht standardmäßig keine Nachricht an sie. | P-49 |
| Verknüpftes Konto wählt „Das bin ich nicht“ | Das ist keine Besetzungsänderung: Nur die Kontoverknüpfung des Slots wird gelöst, Name und Besetzung bleiben unverändert (E-22). | P-60 |
| Teammitglied versucht, die Besetzung online zu ändern | Serverseitig abgelehnt; keine Änderung. | P-61 |
| Formationsstärke überschritten | Online beim Speichern abgelehnt; lokal Warnung, beim Abgleich als nicht übertragbar mit Grund gezeigt. | P-51 |
| Supermêlée, Mêlée, Tête-à-tête | Jede Anmeldung ist genau eine Person; es gibt keine Teambesetzung. Personenzahl 2 wird abgelehnt. | P-52 |


## Ziel und Beteiligte

| Rolle | Aufgabe und Rechte |
| - | - |
| Turnierersteller (Veranstalter) | Konto, das das Online-Turnier angelegt hat. Pflegt die Ausschreibung, bestätigt und storniert Anmeldungen, korrigiert E-Mails, ändert online die Teambesetzung, kann die Dokumentbindung lösen und das Turnier löschen. |
| Admin | Plattform-Administrator mit allen Rechten des Turniererstellers für jedes Turnier. |
| Mitverwalter | Konto mit Verwaltungsberechtigung für das Turnier (Voraussetzung 2). Hat dieselben Rechte wie der Turnierersteller. |
| Turnierleitung | Person(en), die PTM am Turniertag bedienen: Abgleich, Meldeliste, Check-in, Mêlée-Übernahme, Runden. Online handelt PTM über das API-Key-Konto des verbundenen Dokuments. Wo dieses Dokument von Aktionen der Turnierleitung in PTM Online spricht (bestätigen, stornieren, E-Mails korrigieren), handelt sie mit einem Konto als Turnierersteller, Mitverwalter oder Admin. |
| Absendendes Konto | Konto, das eine Online-Anmeldung abschickt. Hat keine Sonderrolle (E-22). |
| Teammitglied mit Konto | Mit einem Personen-Slot verknüpft. Sieht die Live-Ansicht seiner Anmeldung und kann sich mit „Das bin ich nicht“ lösen; ändert nichts an der Anmeldung. |
| PTM Online | Nimmt Anmeldungen entgegen, verwaltet Status und stellt die Meldungen für den Abgleich bereit. |
| PTM | Führt die lokale Meldeliste, die Auslosung und die Turnierdurchführung. |


## User Story

> Als Veranstalter möchte ich Anmeldungen vor dem Turnier über PTM Online sammeln und sie in meine PTM-Meldeliste übernehmen, damit ich am Turniertag mit einer geprüften Liste arbeiten kann. Neue Online-Anmeldungen sollen nicht versehentlich als anwesend gelten. Lokale Nachmeldungen sollen umgekehrt nach PTM Online übertragen werden können.

### Akzeptanzkriterien

- Ein PTM-Dokument kann nur mit einem passenden, für den API-Key berechtigten Online-Turnier verbunden werden.

- Eine bestätigte Online-Anmeldung wird nach einem manuellen Abgleich in die passende lokale Anmeldeliste übernommen.

- Eine übernommene Anmeldung ist zunächst **nicht eingecheckt**.

- Eine lokal erfasste Meldung wird beim Abgleich als Anmeldung in PTM Online angelegt, sofern dort keine gleichartige Anmeldung besteht.

- Der Abgleich zeigt nachvollziehbar an, wie viele Meldungen lokal übernommen und wie viele online angelegt wurden.

- Vor dem Start der ersten Runde warnt PTM, falls bestätigte Online-Anmeldungen noch nicht in der Meldeliste stehen.

- Während der Verbindung darf nicht unbemerkt ein zweites Turnierdokument denselben Online-Turnierstand beschreiben: Die Bindung ist 1:1.

## Voraussetzungen für die Ausführung

1. In PTM Online existiert ein veröffentlichungsfähiges Turnier mit dem passenden Turniersystem und der passenden Anmeldeart.

2. Das Turnier gehört dem Konto des verwendeten API-Keys, oder dieses Konto hat die Berechtigung zur Verwaltung.

3. In PTM ist das Turnier erzeugt und die zugehörige Meldeliste vorhanden.

4. Unter den PTM-Optionen sind **Base-URL** (standardmäßig `https://ptmonline.org`) und der freigeschaltete **API-Key** hinterlegt. Die Aktion „Verbindung testen“ muss erfolgreich sein.

5. Das PTM-Dokument ist die aktuelle Arbeitsdatei und wird regelmäßig gespeichert.

Die Implementierung muss die Verbindung für Supermêlée, Liga, Maastrichter, Schweizer System, Jeder-gegen-Jeden, K.-o., Poule A/B, Kaskade, Formule X und Trip-Tête ermöglichen. Die Auswahlliste darf nur Online-Turniere anzeigen, deren System und Anmeldeart zur offenen PTM-Datei passen. Die Anmeldeart unterscheidet Einzelanmeldung (Supermêlée, Mêlée-Anmeldung, Tête-à-tête) und Team-Anmeldung (Formée, E-20); eine PTM-Datei mit Mêlée-Anmeldeliste passt nur zu einem Online-Turnier mit Mêlée-Anmeldung.

## Soll-Workflow

```
PTM Online: Turnier anlegen und Anmeldung öffnen        
                    |        
                    v        
PTM: Turnier erzeugen, Zugang testen, Online-Turnier verbinden        
                    |        
                    v        
Spieler/Teams melden sich online an  <-->  Turnierleitung erfasst Nachmeldungen lokal        
                    |                                  |        
                    +------------ PTM-Online > Meldungen abgleichen ------------+        
                                                       |        
                                                       v        
                         geprüfte Meldeliste, noch ohne Check-in        
                                                       |        
                                                       v        
                                  Check-in in PTM  
                                                       |  
                                                       v  
                  nur bei Mêlée: Mêlée-Übernahme (separater Schritt)  
                                                       |  
                                                       v  
                              Start der ersten Runde
```

### 1. Online-Turnier vorbereiten

Der Veranstalter muss das Turnier zuerst in PTM Online anlegen. Dort müssen mindestens Turniername, Termin, System, Formation beziehungsweise Anmeldeart und die Regeln für die Anmeldung gepflegt werden. Die Implementierung darf keine Verbindung zu einem fachlich unpassenden Turnier herstellen.

Bei Supermêlée, Mêlée-Anmeldung und Tête-à-tête meldet sich jede Person einzeln an. Bei Mêlée entstehen die Teams erst nach dem Check-in in PTM, bei Supermêlée erst bei der Auslosung; Einzelanmeldungen dürfen nie vorab als fertiges Team behandelt werden. Bei allen anderen Systemen wird ein Team mit 2 Personen bis zur Formationsstärke angemeldet (E-20). Ein unvollständiges Team ist zulässig und wird vor Ort vervollständigt.

Je nach Einstellung bestätigt die Turnierleitung Online-Anmeldungen in PTM Online oder sie werden automatisch bestätigt. Für die Übernahme nach PTM darf nur eine bestätigte Anmeldung zählen; offene, wartende oder stornierte Einträge dürfen nicht als reguläre lokale Teilnehmer angelegt werden.

### 2. PTM vorbereiten und verbinden

1. Ein neues PTM-Turnier mit demselben Turniersystem anlegen und die Meldeliste erzeugen.

2. In den PTM-Optionen Base-URL und API-Key eintragen und **Verbindung testen** wählen.

3. Im Menü **PTM-Online → Mit Online-Turnier verbinden** das richtige Turnier auswählen.

4. Die erfolgreiche Verbindung bestätigen und das automatisch angelegte Blatt **PTMOnline Sync** nicht für normale Eingaben verwenden.

Die Verbindung enthält eine Dokumentbindung und einen Schreib-Lease. Ein Online-Turnier kann immer nur mit einem PTM-Dokument verbunden sein. Das Übernehmen einer bereits bestehenden Verbindung ist nur sinnvoll, wenn die neue Datei wirklich den aktuellen Stand enthält: Beim nächsten Rundenstart kann sie Online-Teilnahme und Setzpositionen überschreiben.

### 3. Anmeldungen entgegennehmen

Spieler oder Teams melden sich in PTM Online an. Die Turnierleitung kontrolliert dort gegebenenfalls offene Anmeldungen, Warteliste, Stornierungen und die Angaben der bestätigten Meldungen.

Zusätzliche Anmeldungen am Telefon oder am Platz werden in PTM direkt in der Meldeliste erfasst. Sie werden nicht automatisch sofort in PTM Online sichtbar; dafür ist der nächste manuelle Abgleich vorgesehen.

### 4. Meldungen abgleichen

Die Turnierleitung wählt **PTM-Online → Meldungen abgleichen**. Der Abgleich arbeitet in zwei Richtungen:

1. PTM ruft neue oder geänderte bestätigte Online-Anmeldungen ab und übernimmt sie in die lokale Meldeliste.

2. PTM legt lokale Meldungen, die online noch fehlen, als Anmeldung in PTM Online an.

Danach zeigt PTM eine Zusammenfassung, beispielsweise: „3 Meldungen lokal übernommen, 1 online angelegt.“ Alle offenen Fälle stehen gesammelt im Blatt **PTMOnline Konflikte** (A-29): möglicherweise identisch, Namens- und Besetzungskonflikte, online storniert, lokal gelöscht, nach Turnierstart eingegangen, Konto doppelt angemeldet, unvollständige Teams, mögliche Dubletten, nicht übertragene Einträge und vom Server abgelehnte Schreibaufträge. Wo eine Wahl möglich ist, trägt die Turnierleitung sie in der Spalte „Entscheidung“ ein (Auswahlliste, auch im Turnier-Modus bearbeitbar); der nächste Abgleich wendet sie an, protokolliert sie online und schreibt die Liste neu. Ein vom Server abgelehnter Auftrag verschwindet aus der Liste, sobald „Zur Kenntnis genommen“ gewählt ist. Hinweise müssen vor dem Turnierstart bearbeitet werden.

| Hinweis oder Fall | Vorgehen |
| - | - |
| Name ist bereits lokal vorhanden | Prüfen, ob es dieselbe Person beziehungsweise dasselbe Team ist. Bei zwei unterschiedlichen Personen die lokale Bezeichnung eindeutig machen, z. B. „Müller jun.“; dann erneut abgleichen. |
| Namen oder Formation lassen sich nicht eindeutig zuordnen | Schreibweise und Teamzusammensetzung prüfen. PTM versucht die Meldung beim nächsten Abgleich erneut. |
| Lokale Meldung konnte online nicht angelegt werden („möglicherweise identisch“) | Online existiert eine gleichnamige, noch nicht zugeordnete Anmeldung. Diese entweder mit der lokalen Meldung verknüpfen oder bewusst als getrennt markieren (KP-06 a2); danach erneut abgleichen. |
| Abgleich pausiert | Im Menü **PTM-Online → Sync fortsetzen** wählen und anschließend erneut abgleichen. |
| Netzwerk- oder Berechtigungsfehler | Verbindung testen, API-Key und Berechtigung prüfen; lokale Turnierarbeit kann weitergehen, der Abgleich wird später wiederholt. |


Der manuelle Abgleich ist bewusst der kontrollierte Zeitpunkt für den Import. Ein Rundenstart importiert nicht automatisch stillschweigend.

### 5. Meldeliste prüfen und Check-in durchführen

Nach jedem Abgleich prüft die Turnierleitung die Meldeliste: vollständige Namen und Teams, Dubletten, Status und eventuell notwendige Nachmeldungen. Erst am Turniertag wird in PTM der **Check-in** gesetzt.

| Zustand | Bedeutung |
| - | - |
| bestätigt (PTM Online) | Anmeldung ist organisatorisch akzeptiert; die Person oder das Team kann in PTM übernommen werden. |
| nicht eingecheckt (PTM) | Die Meldung ist vorhanden, nimmt aber noch nicht an der Auslosung teil. Dies ist der Anfangszustand einer aus PTM Online importierten Meldung. |
| eingecheckt/aktiv (PTM) | Die Person oder das Team ist anwesend und nimmt an der nächsten Runde teil, sofern kein Ausschlussgrund besteht (Konflikt nach KP-06, unvollständiges Team, „online storniert“). |
| ausgesetzt (PTM) | Die Meldung bleibt bekannt, nimmt aber aktuell nicht teil. |


Beim Check-in darf die Turnierleitung die Besetzung eines Formée-Teams ändern (Ersatz, fehlende Person). Unvollständige Teams nehmen nicht an der Auslosung teil, bis sie vervollständigt sind (KP-20). Bei Mêlée folgt nach Abschluss des Check-ins und kurz vor dem Turnierstart der separate Schritt **Mêlée-Übernahme**, der Teams aus den eingecheckten Einzelanmeldungen bildet; ein erneutes Ausführen verwendet nur noch nicht zugeordnete Personen, gebildete Teams bleiben unverändert (KP-18).

### 6. Erste Runde starten und Turnier durchführen

Beim Start der ersten Runde führt PTM den Vorabcheck nach KP-05 aus: fehlende bestätigte Online-Anmeldungen, offene Konflikte und bei Mêlée die ausstehende Mêlée-Übernahme. Bei einem Befund bietet PTM an, den Start abzubrechen und die passende Aktion auszuführen: **Meldungen abgleichen**, Konflikt auflösen oder **Mêlée-Übernahme**. Diese Prüfung dient als Schutz vor einer versehentlich unvollständigen Auslosung.

Nach dem Start übermittelt PTM den lokalen Teilnahmezustand und die Setzpositionen an PTM Online. Das PTM-Dokument ist für diese Turnierdaten maßgeblich. Der laufende Abgleich erfolgt so, dass die Auslosung nicht auf eine Netzwerkantwort warten muss; eine gestörte Verbindung darf die lokale Runde nicht blockieren.

## So soll der Betrieb laufen

Für ein normales Turnier gibt es einen klaren Takt. Er verhindert, dass die Turnierleitung gleichzeitig in zwei Oberflächen dieselbe Information pflegt.

| Zeitpunkt | Verbindliche Aktion | Führendes System |
| - | - | - |
| Vor Veröffentlichung | Online-Turnier vollständig einrichten, PTM-Datei erzeugen, Verbindung testen und herstellen. | PTM Online für Ausschreibung; PTM für die Turnierdatei |
| Anmeldephase | Anmeldungen und Bestätigungen in PTM Online pflegen; regelmäßigen manuellen Abgleich in PTM durchführen. | PTM Online für den Anmeldestatus |
| Unmittelbar vor Turnierbeginn | Letzten Abgleich ausführen, Meldeliste kontrollieren, dann ausschließlich den Check-in in PTM setzen. | PTM für Anwesenheit |
| Nach Abschluss des Check-ins, nur bei Mêlée | Separaten Schritt „Mêlée-Übernahme“ ausführen und die Teams kontrollieren. | PTM |
| Rundenstart und laufendes Turnier | Runden und Ergebnisse in PTM führen; PTM überträgt Teilnahme und Setzpositionen. | PTM |
| Nach dem Turnier | Verbindung kontrolliert trennen, wenn das Turnier abgeschlossen ist; Dokument und Sync-Blatt archivieren. | PTM |


Die zentrale Regel lautet: **Eine Anmeldung ist keine Anwesenheit.** PTM Online verwaltet, wer angemeldet und bestätigt ist. PTM entscheidet durch den Check-in, wer tatsächlich ausgelost wird. Eine Änderung wird stets zuerst dort vorgenommen, wo sie fachlich hingehört, und danach kontrolliert abgeglichen.

## Risiken und erforderliches Fehlerverhalten

Die folgende Tabelle ist verbindlich für die Implementierung: In jeder aufgeführten Situation muss die Anwendung die angegebene Wirkung vermeiden und die Turnierleitung handlungsfähig informieren. Ein Fehlerdialog ohne nächste sinnvolle Aktion erfüllt die Anforderung nicht.

| Risiko oder Fehlersituation | Auswirkung | Erkennung | Sofortmaßnahme | Vorbeugung |
| - | - | - | - | - |
| Internet oder PTM Online nicht erreichbar | Neue Online-Meldungen und Statusänderungen erreichen PTM nicht; das lokale Turnier kann weitergeführt werden. | Netzwerk-/Serverfehler, fehlender erfolgreicher Abgleich. | Nicht wiederholt blind starten. PTM-Datei speichern, Sync pausieren, lokal weiterarbeiten; nach Rückkehr Sync fortsetzen und manuell abgleichen. | Verbindung am Vortag und vor Öffnung der Anmeldung testen; lokale Kopie der Meldeliste bereithalten. |
| API-Key fehlt, ist ungültig oder nicht freigeschaltet | Verbinden und Abgleich scheitern. | Fehler beim Verbindungstest oder Berechtigungsmeldung. | API-Key und Konto-Berechtigung prüfen; bei Bedarf PTM-Online-Administration einbeziehen. | API-Key früh eintragen und erfolgreich testen, nicht erst am Turniertag. |
| Falsches Online-Turnier oder falsches System | Eine Verbindung kommt nicht zustande oder Meldungen würden fachlich nicht passen. | Auswahlliste enthält das Turnier nicht; System-/Anmeldeart passt nicht. | Verbindung abbrechen, Online-Turnier und lokale PTM-Datei prüfen. | Gleichen Namen, Termin, System und Anmeldeart (Einzel- oder Team-Anmeldung) vor dem Verbinden kontrollieren. |
| Zwei PTM-Dateien sollen dasselbe Online-Turnier steuern | Gefahr, dass ein veraltetes Dokument Teilnahme oder Setzpositionen überschreibt. | Hinweis, dass das Turnier bereits mit einem anderen Dokument verbunden ist, oder verlorene Bindung. | Keine Übernahme „auf Verdacht“. Nur die nachweislich aktuelle Datei verwenden; gegebenenfalls alte Datei korrekt trennen. | Eine Arbeitsdatei als verbindlich festlegen; keine Kopien parallel einsetzen. |
| Lokale Datei hat die Bindung verloren | Schreibvorgänge werden abgewiesen, weil ein anderes Dokument die Verbindung übernommen hat. | Meldung, dass dieses Dokument nicht mehr verbunden ist. | Lokale Arbeit speichern; prüfen, welches Dokument aktuell ist. Dieses entweder verbinden oder die alte Bindung kontrolliert lösen. | Dokumentbindung und Lease nicht manuell im Sync-Blatt ändern. |
| Bestätigte Online-Anmeldung fehlt in PTM | Team oder Spieler fehlt möglicherweise in der Auslosung. | Warnung vor Rundenstart; Abgleich meldet nicht übernommene Einträge. | Rundenstart abbrechen, **Meldungen abgleichen** ausführen und Meldeliste prüfen. | Letzten manuellen Abgleich als festen Punkt vor dem Check-in vorsehen. |
| Gleichnamige Personen oder Teams | PTM könnte die falsche Meldung zuordnen; daher wird sie nicht übernommen. | Hinweis zu Namensgleichheit oder nicht eindeutiger Zuordnung. | Nicht die Zuordnung erzwingen. Lokalen Namen eindeutig machen, z. B. mit Verein oder „jun.", dann erneut abgleichen. | Eindeutige Namen bereits in der Online-Anmeldung verlangen oder früh bereinigen. |
| Team wurde lokal umsortiert oder Namen in einer Zeile geändert | Online-IDs könnten scheinbar zu falschen Teams gehören. | Abweichender Name an einer Zeile mit Online-ID. | Sortieren ist unkritisch, weil die UUID bei der Meldung bleibt (T-03/T-04). Ein abweichender Name gilt als Änderung dieses Personen-Slots, also als Korrektur oder Personenwechsel innerhalb des Teams (A-14). Ein Teamtausch ist nicht vorgesehen: Soll ein Team ersetzt werden, wird die alte Meldung storniert und die neue als neue Meldung erfasst. | Keine IDs händisch kopieren; ein anderes Team nicht durch Überschreiben aller Personen einer bestehenden Zeile „eintauschen“. |
| Unvollständiges Team wird ausgelost oder Mêlée-Einzelanmeldungen als fertiges Team behandelt | Falsche Teilnahme oder Team mit fehlender Person in der Runde. | Formée-Team hat weniger Personen, als die Formation verlangt; Mêlée-Einzelanmeldungen ohne Teamzuordnung. | Formée-Team vervollständigen; bei Mêlée nur eingecheckte Einzelanmeldungen über „Mêlée-Übernahme“ zusammenstellen; bis dahin nicht auslosen. | Anmeldeeinheit (E-20) in der Ausschreibung erklären; fehlende Personen früh klären. |
| Online-Anmeldung ist offen, wartet oder storniert | Sie erscheint nicht als reguläre lokale Meldung. | Status in PTM Online; keine Übernahme beim Abgleich. | Status bewusst in PTM Online bearbeiten; danach erneut abgleichen. | Zuständigkeit für Bestätigung und Warteliste vorab festlegen. |
| Revisionskonflikt bei Live-Check-in | Eine konkurrierende Online-Änderung wird nicht endlos überschrieben. | Statusänderung wird verworfen; späterer Rundenstart gleicht vollständig ab. | Lokalen Check-in prüfen; beim nächsten Rundenstart beziehungsweise nach stabilem Netz vollständig abgleichen. | Check-in ausschließlich in PTM durchführen, sobald das Turnier läuft. |
| Kopie der verbundenen PTM-Datei wird parallel benutzt | Zwei Dateien schreiben mit derselben Bindung; ein veralteter Stand kann Online-Daten überschreiben. | Server lehnt mit `document_forked` ab (E-24, T-19). | Klären, welche Datei aktuell ist; die andere über die Übernahme ausschließen. | Verbundene Datei nicht kopieren oder weitergeben; Backups nur zur Wiederherstellung nutzen. |
| Sync-Blatt beschädigt oder Zeilen gelöscht | Zuordnungen fehlen; drohende Dubletten oder falsche Konflikte. | Abgleich findet Meldungen ohne Zuordnung oder Zuordnungen ohne Meldung. | „Zuordnung vom Server wiederherstellen“ ausführen (T-21), danach abgleichen. | Sync-Blatt nicht bearbeiten; Blattschutz aktiv lassen. |
| Testlauf setzt das Turnier versehentlich auf `running` | Online-Anmeldung geschlossen. | Warnung vor dem ersten `running`; Status `running` in PTM Online. | Turnierersteller, Mitverwalter oder Admin setzen `running` zurück, solange kein Rundenergebnis vorliegt (E-23). | Testläufe mit einer Kopie ohne Verbindung durchführen. |
| Online-Turnier wurde gelöscht | Synchronisation endet, das lokale Turnier bleibt aber lauffähig. | PTM meldet die Löschung; Sync-Blatt wird archiviert. | Lokal weiterführen; nur falls erforderlich ein neues passendes Online-Turnier verbinden. | Löschrechte beschränken und das Online-Turnier erst nach Abschluss löschen. |


### Vorgehen bei einem Fehler

1. **Stoppen und sichern:** PTM-Datei speichern und den genauen Fehlertext notieren oder fotografieren.

2. **Fachliche Wirkung bestimmen:** Betrifft der Fehler nur die Online-Anzeige, die Anmeldung oder die aktuelle Auslosung? Eine bereits erzeugte Runde wird nicht durch einen fehlgeschlagenen Online-Aufruf ungültig.

3. **Keine manuelle Zuordnung erzwingen:** Insbesondere keine Online-IDs, Revisionen oder Lease-Werte im Blatt `PTMOnline Sync` kopieren, löschen oder umhängen.

4. **Wiederherstellen:** Bei Netzwerkproblemen Sync pausieren und später fortsetzen. Bei Status-, Namens- oder Teamproblemen die Daten an der fachlichen Quelle korrigieren und erneut abgleichen.

5. **Vor nächster Runde prüfen:** Meldeliste, Check-in und die Zusammenfassung des letzten Abgleichs kontrollieren. Bei fehlenden bestätigten Online-Meldungen die Runde nicht starten, bevor die Turnierleitung die Abweichung bewusst entschieden hat.

## Fachliche Anforderungen

| ID | Anforderung | Priorität |
| - | - | - |
| A-01 | PTM muss nur passende, für den API-Key verwaltbare Online-Turniere zur Verbindung anbieten. | Muss |
| A-02 | Eine Verbindung muss pro Online-Turnier genau einem aktiven PTM-Dokument zugeordnet sein. | Muss |
| A-03 | Der Abgleich muss bestätigte Online-Anmeldungen in die richtige PTM-Meldeliste übernehmen. | Muss |
| A-04 | Übernommene Online-Anmeldungen müssen in PTM zunächst inaktiv sein; Anmeldestatus und Check-in sind getrennt. | Muss |
| A-05 | Der Abgleich muss lokale, noch nicht vorhandene Meldungen online anlegen, solange das Online-Turnier nicht `running` ist. | Muss |
| A-06 | PTM muss Import, Online-Anlage und nicht bearbeitete Fälle für die Turnierleitung sichtbar zusammenfassen. | Muss |
| A-07 | Bei Namensgleichheit oder unklarer Formation darf PTM keine Zuordnung erraten oder Daten überschreiben. | Muss |
| A-08 | Vor der ersten Runde muss PTM auf bestätigte, aber noch nicht lokal übernommene Online-Meldungen hinweisen. | Muss |
| A-09 | Die Turnierleitung muss den Sync pausieren und fortsetzen können, ohne die Verbindung zu verlieren. | Muss |
| A-10 | Einzelanmeldungen (Supermêlée, Mêlée, Tête-à-tête) müssen online und lokal Einzelanmeldungen bleiben, auch nachdem PTM daraus Mêlée-Teams gebildet hat. Unvollständige Formée-Teams müssen importiert, als „unvollständig“ gekennzeichnet und bis zur Vervollständigung von der Auslosung ausgeschlossen werden. | Muss |
| A-11 | Fehler bei Netz, API-Key oder Serverantwort müssen verständlich angezeigt werden; lokale Turnierarbeit darf dadurch nicht verloren gehen. | Muss |
| A-12 | Das technische Blatt `PTMOnline Sync` muss Zuordnungen und letzten Sync nachvollziehbar dokumentieren, ist aber kein reguläres Erfassungsblatt. | Muss |
| A-13 | Ein manueller Abgleich muss idempotent sein: Wiederholen nach Abbruch oder Netzfehler darf keine Anmeldung doppelt anlegen. | Muss |
| A-14 | Ein Teamtausch (zwei Meldungen tauschen ihre Identität bzw. Online-ID) ist kein unterstützter Anwendungsfall; PTM darf keine Tauscherkennung durchführen. Änderungen der Besetzung innerhalb eines Teams sind dagegen zulässig (E-20): Ein geänderter Name in einem Personen-Slot ohne Benutzer-ID wird als Änderung dieses Slots übertragen; eine geänderte Benutzer-ID bedeutet Personenwechsel. Soll ein ganzes Team ersetzt werden, erfolgt das über Storno und Neuerfassung. | Muss |
| A-15 | Ein Revisionskonflikt darf nicht zu automatischen Endloswiederholungen oder zum Überschreiben einer neueren Online-Änderung führen. | Muss |
| A-16 | Schreibende Synchronisation muss eine gültige Dokumentbindung und einen Schreib-Lease verlangen. Verliert ein Dokument diese Bindung, darf es keine Daten mehr online verändern. | Muss |
| A-17 | Sichtbarkeit und Anmeldeberechtigung müssen getrennt sein: private Turniere sind nur über einen gültigen Freigabelink anmeldbar, öffentliche über die normale URL; Entwürfe sind nie anmeldbar. | Muss |
| A-18 | Mit dem atomaren Übergang zu `running` muss PTM Online neue Anmeldungen für alle Zugangswege sperren. | Muss |
| A-19 | Eine verlorene PTM-Datei muss über eine bestätigungspflichtige Bindungsübernahme wiederherstellbar sein; die alte Schreibberechtigung muss dabei atomar erlöschen. | Muss |
| A-20 | Check-in muss ausschließlich in PTM erfolgen und für das zugehörige Konto in der Live-Ansicht zeitnah sichtbar werden. Er darf nie eine E-Mail auslösen; die Push-/Postbox-Nachricht an verknüpfte Konten ist Standard und pro Turnier abschaltbar. | Muss |
| A-21 | Konto-Zuordnung und Live-Berechtigung müssen eine stabile Benutzer-ID verwenden. Eine Änderung der Profil-E-Mail darf weder die Anmeldung noch Live-Zugriff oder konto-bezogene Benachrichtigungen lösen. | Muss |
| A-22 | Eine durch die Turnierleitung korrigierte Kontakt- oder Slot-E-Mail muss auf die betreffende Anmeldung begrenzt bleiben und darf kein Benutzerprofil ändern. Die Kontakt-E-Mail darf nie ein Konto verknüpfen; eine Slot-E-Mail verknüpft nur einen unverknüpften Slot nach E-22. | Muss |
| A-23 | Online stornierte oder auf die Warteliste gesetzte, bereits importierte Meldungen müssen lokal markiert und bis zur Entscheidung der Turnierleitung von der Auslosung ausgeschlossen werden; sie dürfen nicht automatisch gelöscht werden. | Muss |
| A-24 | Das lokale Löschen einer verknüpften Meldung darf online nichts ohne ausdrückliche Wahl der Turnierleitung ändern; eine nur lokal entfernte Meldung darf nicht erneut importiert werden. | Muss |
| A-25 | Beidseitige, unterschiedliche Änderungen an Namen oder Teambesetzung müssen als Konflikt angezeigt werden; keine Seite darf die andere still überschreiben. | Muss |
| A-26 | Mêlée-Teams müssen ausschließlich über die vorhandene Mêlée-Übernahme als separaten Schritt aus eingecheckten, noch nicht zugeordneten Einzelanmeldungen gebildet werden; bestehende Teams bleiben unverändert. | Muss |
| A-27 | Für registrierte Teilnehmer muss die Benutzer-ID in PTM und PTM Online der Personenschlüssel sein, für Dublettenerkennung, automatische Verknüpfung, Live-Ansicht und Benachrichtigung. In dieser Ausbaustufe bezieht PTM sie ausschließlich aus PTM Online. | Muss |
| A-28 | Die Anmeldeeinheit muss System und Anmeldeart folgen: genau eine Person bei Supermêlée, Mêlée und Tête-à-tête, sonst ein Team mit 2 Personen bis zur Formationsstärke. Besetzungsänderungen von Formée-Teams während der Anmeldephase und beim Check-in müssen in beide Richtungen abgeglichen werden, ohne die Identität des Teams zu ändern. | Muss |
| A-29 | PTM muss alle offenen Fälle (möglicherweise identisch, Namens- und Besetzungskonflikte, Doppelbelegungen, online storniert, nach Turnierstart eingegangen, unvollständige Teams, Mêlée-Nachzügler, nicht übertragbare Einträge) in einer gesammelten Konfliktliste zeigen, statt einzelner aufeinanderfolgender Dialoge. Den Rundenstart halten nur echte Ausschlussgründe an; reine Hinweise blockieren nicht. | Muss |
| A-30 | Datenschutz: Die Anforderungen im Abschnitt „Datenschutz“ sind verbindlich. | Muss |


## Technische Implementierungsanforderungen

| ID | Anforderung | Priorität |
| - | - | - |
| T-01 | Die Verbindung muss serverseitig atomar erzeugt werden. Zwei gleichzeitig gestartete Verbindungsversuche dürfen nicht zu zwei schreibberechtigten PTM-Dokumenten führen. | Muss |
| T-02 | Jeder schreibende Aufruf von PTM an PTM Online muss die Dokument-ID und den Lease-Token übermitteln; der Server muss beides prüfen. | Muss |
| T-03 | Für jede lokale Meldung muss eine stabile lokale UUID gespeichert werden. Zeilennummer, Sortierung, Name und E-Mail-Adresse dürfen nicht als Identität verwendet werden. Die UUID identifiziert die Meldung (Team), die Benutzer-ID den einzelnen registrierten Spieler (T-17). | Muss |
| T-04 | Die Zuordnung lokale UUID ↔ Online-Anmeldungs-ID muss im technischen Sync-Speicher gehalten werden, mit dem Inhalt nach Abschnitt „Identitäten und Mapping-Tabelle“. Beim Sortieren der Meldeliste muss die UUID bei der Meldung bleiben. | Muss |
| T-05 | Online-Importe dürfen nur im explizit ausgelösten Meldungsabgleich die Meldeliste verändern. Der Rundenstart darf fehlende Meldungen prüfen und warnen, aber nicht verdeckt importieren. | Muss |
| T-06 | Ein Import muss alle bestätigten Anmeldungen mit gültiger Anmeldeeinheit anlegen (1 Person bzw. 2 Personen bis zur Formationsstärke), unvollständige Formée-Teams gekennzeichnet. Anmeldungen mit falscher Personenzahl oder nicht zuordenbare Einträge müssen mit Grund sichtbar bleiben und beim nächsten Abgleich erneut prüfbar sein. | Muss |
| T-07 | Der Check-in-Push und andere Netzaufrufe dürfen die LibreOffice-Oberfläche, die Auslosung oder UNO-Zugriffe nicht blockieren. UI-Änderungen müssen auf dem LibreOffice-Hauptthread erfolgen. | Muss |
| T-08 | Bei Fehlern muss der zuletzt erfolgreich bekannte Sync-Zeitpunkt unverändert bleiben, sofern der Import nicht vollständig verarbeitet wurde. | Muss |
| T-09 | PTM muss alle lokalen Änderungen, die während einer Unterbrechung entstehen, puffern und danach vollständig übertragen, auch wenn sie gegenüber dem zuletzt gemerkten Stand gleich wirken. Das gilt für eine bewusste Pause (A-09) ebenso wie für einen Netz- oder Serverausfall ohne Pause (KP-05, KP-09). Der Puffer muss mit dem Dokument gespeichert werden und einen Neustart von LibreOffice überstehen. Aufträge werden nur auf dem LibreOffice-Hauptthread aus einem Dokument-Snapshot erzeugt, fertig serialisiert gespeichert und erst dann dem Hintergrundversand übergeben; der Hintergrund liest und schreibt keine Dokumentinhalte. Beim lokalen Turnierstart werden ungesendete Vorstart-Aufträge verworfen (KP-05). | Muss |
| T-10 | Benutzertexte, Fortschritt, Warnungen und Fehler müssen lokalisiert und so konkret sein, dass die Turnierleitung Ursache und nächste Aktion erkennt. | Muss |
| T-11 | Freigabelinks für private Turniere müssen widerrufbar sein; im Speicher darf nur ihr kryptografischer Hash liegen. Die Berechtigung muss beim Detailabruf, beim Anmeldeformular und beim Absenden serverseitig geprüft werden. | Muss |
| T-12 | Der Übergang zu `running` und die Sperre neuer Anmeldungen müssen in einer atomaren Serveroperation erfolgen, damit parallele Anmeldeversuche nicht nach dem Start durchrutschen. Da D1 keine interaktiven Transaktionen bietet, müssen alle Prüfungen aus KP-02 (Status, Anmeldeschluss, Kapazität, Doppelbelegung) bzw. für den PTM-Zugangsweg die Prüfungen aus T-24 (Bindung, Lease, Zähler, Status, Kennzeichnung `over_capacity`) jeweils zusammen mit dem Anlegen als bedingte Einzelbefehle bzw. als D1-Batch umgesetzt werden, deren Wirkung über die Zahl der betroffenen Zeilen geprüft wird. Nebenläufigkeitstests mit parallelen Anmeldungen sind Pflicht. Dasselbe gilt für die atomare Verbindung (T-01) und die Übernahme (A-19). | Muss |
| T-13 | Die Live-Ansicht muss inhaltlich an der Online-Anmeldungs-ID hängen (für alle Personen der Anmeldung identisch) und den Zugriff ausschließlich anhand der stabilen Benutzer-IDs der verknüpften Personen-Slots autorisieren; das absendende Konto hat keine Sonderrolle. Die Kontakt-E-Mail einer Anmeldung darf nicht als Login- oder Live-Berechtigung dienen. | Muss |
| T-14 | Änderungen an Kontakt- und Slot-E-Mails, Kontoverknüpfungen (automatisch, „Das bin ich nicht“, „Konto neu zuordnen“), manuellen Besetzungsänderungen, Anmeldestatus, Entscheidungen zu online stornierten Meldungen, Bindungsübernahme, Lösen der Bindung und Löschung müssen mit Zeitpunkt, handelnder Rolle und Zielobjekt revisionssicher protokolliert werden. | Muss |
| T-15 | Das Sync-Blatt muss pro Meldung den zuletzt abgeglichenen Namen und Online-Status sowie ausstehende Aufträge (z. B. „online stornieren“) und den Vermerk „lokal entfernt“ speichern. | Muss |
| T-16 | Der Schreib-Lease darf keine zeitliche Gültigkeit haben. Der Server muss ein ausdrückliches, protokolliertes Lösen der Bindung durch den Veranstalter anbieten. | Muss |
| T-17 | PTM muss die Benutzer-ID pro Person im technischen Sync-Speicher an der Meldung halten, sortierstabil und nicht als freie Zelle editierbar. Import, Anlage, Check-in, Runden- und Ranglistenpush müssen sie übertragen; der Server muss jede empfangene Benutzer-ID auf Existenz und auf Zugehörigkeit zur betreffenden Anmeldung prüfen. | Muss |
| T-18 | Die Teambesetzung muss pro Person-Slot (Name, optionale Benutzer-ID) übertragen werden. Das Sync-Blatt muss die zuletzt abgeglichene Besetzung speichern, damit einseitige und beidseitige Änderungen unterscheidbar sind (E-16). Manuelle Online-Besetzungsänderungen in der Weboberfläche sind ausschließlich Turnierersteller, Mitverwaltern und Admins erlaubt, und nur bis Anmeldeschluss bzw. `running`; der Server muss diese Berechtigung prüfen. Übertragungen aus dem verbundenen PTM-Dokument mit gültigem Schreib-Lease (T-02) sind davon ausgenommen und immer zulässig, unabhängig davon, welchem berechtigten Konto der API-Key gehört. Bei Mêlée muss PTM die Teamzuordnung (Team-UUID → Online-Anmeldungs-IDs) nach der Mêlée-Übernahme übertragen. | Muss |
| T-19 | Jeder schreibende Aufruf muss zusätzlich einen fortlaufenden Schreibzähler des Dokuments übermitteln. Der Server speichert den zuletzt angenommenen Zählerstand und lehnt kleinere oder gleiche Stände mit dem Code `document_forked` ab (E-24). Wiederholungen regelt T-23: Der Zähler wird nur für neue Auftrags-IDs geprüft. | Muss |
| T-20 | Datenbankänderungen in PTM Online für diese Anforderung (Personen-Slots, Slot-E-Mails, Idempotenzschlüssel, Löschnachweis, Schreibzähler) dürfen nur per ADD COLUMN oder neuer Tabelle erfolgen, nie per Neuaufbau einer Tabelle mit eingehenden ON-DELETE-CASCADE-Fremdschlüsseln. Nach jeder Migration auf dem Server sind die Zeilenzahlen betroffener Tabellen zu prüfen. | Muss |
| T-21 | PTM muss eine Funktion „Zuordnung vom Server wiederherstellen“ anbieten, die die Zuordnung UUID ↔ Online-Anmeldungs-ID sowie den zuletzt abgeglichenen Stand anhand der serverseitig gespeicherten Idempotenzschlüssel neu aufbaut, wenn das Sync-Blatt beschädigt oder unvollständig ist. | Muss |
| T-22 | Übernahme der Bindung, Lösen der Bindung, Zurücksetzen von `running` und Löschen eines verbundenen Turniers müssen dem Turnierersteller per Postbox mitgeteilt werden, wenn ein anderes Konto (Mitverwalter oder Admin) sie auslöst. | Muss |
| T-23 | Jeder schreibende Auftrag von PTM erhält beim Entstehen eine eigene Auftrags-ID (UUID), die zusammen mit Zählerstand und Nutzlast im Puffer (T-09) gespeichert wird, damit ein Wiederholungsversuch auch nach einem Neustart dieselbe ID verwendet. Der Server speichert pro Turnier Auftrags-ID, Hash der Nutzlast und die erzeugte Antwort. Kommt dieselbe Auftrags-ID mit identischem Hash erneut, liefert der Server die gespeicherte Antwort ohne erneute Wirkung und ohne Zählerprüfung. Kommt dieselbe Auftrags-ID mit anderem Hash, lehnt er mit `idempotency_mismatch` ab und ändert nichts. Nur eine neue Auftrags-ID wird gegen den Schreibzähler geprüft (T-19). Die Einträge werden bis zur Löschfrist nach DS-04 aufbewahrt. Absturzsicherheit: Die Registrierung der Auftrags-ID (mit Prüfung von Zähler und Bindung), die fachlichen Änderungen und der Abschluss mit gespeicherter Antwort laufen in genau einer Datenbanktransaktion; die Antwort wird aus dem Zustand nach den Änderungen berechnet. Einen sichtbaren Zwischenzustand „in Bearbeitung“ gibt es nicht. Die lokale UUID als Idempotenzschlüssel der Online-Anlage (Abschnitt „Identitäten und Mapping-Tabelle“) bleibt davon unberührt. | Muss |
| T-24 | Die Online-Anlage lokaler Meldungen durch PTM ist ein eigener, lease-gebundener Zugangsweg und nicht der Anmeldeweg nach KP-02. Er prüft: gültige Bindung, Lease und Schreibzähler (T-02, T-19), Status nicht `running` (A-05), Anmeldeeinheit (E-20) und Idempotenz über die lokale UUID. Er ist ausgenommen von Kapazität, Warteliste, Anmeldeschluss, automatischem Anmeldeschluss zum Turnierbeginn und Freigabelink. Überschreitet die Anlage die Kapazität, setzt der Server in derselben atomaren Operation die Kennzeichnung `over_capacity` und schreibt einen Audit-Eintrag (T-14). Eine Doppelbelegung einer Benutzer-ID führt wie online zum Konflikt (KP-06 b). | Muss |


## Prüffälle für die Abnahme

| ID | Ausgangslage | Aktion | Erwartetes Ergebnis |
| - | - | - | - |
| P-01 | Passendes Online-Turnier, gültiger API-Key | Verbinden | Verbindung entsteht, Sync-Speicher enthält Dokumentbindung und Lease; nur dieses Dokument darf schreiben. |
| P-02 | Drei bestätigte Online-Teams, lokale Liste leer | Meldungen abgleichen | Genau drei lokale, inaktive Meldungen werden angelegt; keine ist eingecheckt. |
| P-03 | Eine offene und eine bestätigte Online-Anmeldung | Meldungen abgleichen | Nur die bestätigte Anmeldung wird lokal angelegt. |
| P-04 | Lokale Nachmeldung ohne Online-ID | Meldungen abgleichen, danach denselben Abgleich wiederholen | Die Meldung wird genau einmal online angelegt und anschließend verknüpft. |
| P-05 | Zwei unterschiedliche Meldungen mit gleichem Namen | Meldungen abgleichen | Keine unsichere Zuordnung; sichtbarer Hinweis mit Korrekturweg. |
| P-06 | Verbindung während des Imports unterbrochen | Abgleich nach Wiederherstellung erneut starten | Keine Duplikate; nicht vollständig verarbeitete Meldungen werden erneut geprüft. |
| P-07 | Bestätigte Online-Meldung fehlt lokal | Erste Runde starten | PTM warnt sichtbar und bietet an, den Start abzubrechen und abzugleichen. |
| P-08 | Zwei PTM-Dateien verbinden sich gleichzeitig | Beide verbinden | Höchstens eine erhält die Schreibberechtigung; die andere erhält einen Konflikt statt einer stillen Übernahme. |
| P-09 | Meldeliste wurde nach der Verbindung umsortiert | Abgleich | Alle Zuordnungen bleiben über die UUID erhalten; online ändert sich nichts. |
| P-10 | Name in einer Zeile mit Online-ID wurde lokal korrigiert | Abgleich | Die zugeordnete Online-Anmeldung erhält den korrigierten Namen; es entsteht keine neue Anmeldung und keine Tauschlogik greift. |
| P-11 | Sync ist pausiert, Check-ins ändern sich | Sync fortsetzen | Alle während der Pause aufgelaufenen Änderungen werden übertragen. |
| P-12 | Netzfehler beim Rundenstart | Runde erzeugen | Die Runde wird lokal nicht blockiert; der Fehler wird sichtbar dokumentiert. |
| P-13 | Privates, freigegebenes Turnier | Ohne und mit gültigem Freigabelink anmelden | Ohne Link wird der Zugriff abgelehnt; mit Link ist die Anmeldung bis zum Start möglich. |
| P-14 | Turnierstatus wechselt parallel zum Absenden einer Anmeldung auf `running` | Anmeldung absenden | Anmeldung wird serverseitig abgelehnt und nicht angelegt. |
| P-15 | Die ursprüngliche PTM-Datei ist verloren | Neues Dokument verbindet und bestätigt die Übernahme | Alte Bindung verliert Schreibrecht; bestätigte Online-Meldungen werden importiert; Hinweis auf nicht synchronisierte lokale Daten erscheint. |
| P-16 | Teilnehmer ändert verifizierte Profil-E-Mail | „Meine Live-Turniere“ öffnen und eine konto-bezogene Nachricht auslösen | Bestehende Anmeldung bleibt sichtbar; Nachricht geht an die neue Profil-E-Mail. |
| P-17 | Turnierleitung korrigiert Kontakt-E-Mail einer Anmeldung | Korrektur speichern | Nur diese Anmeldung ändert sich; Benutzerprofil und fremde Kontoverknüpfungen bleiben unverändert; Audit-Eintrag entsteht. |
| P-18 | Teilnehmer wurde lokal eingecheckt | Live-Ansicht seiner Anmeldung öffnen | Ansicht aktualisiert Status „eingecheckt“ und zeigt ab Auslosung Team, Runde, Gegner und Bahn; es geht keine E-Mail raus; bei Standardeinstellung genau eine Push-/Postbox-Nachricht an das Konto, bei abgeschalteter Option keine. |
| P-19 | Privates Turnier, Formular über Freigabelink geöffnet | Link widerrufen, dann Formular absenden | Ablehnung „Link nicht mehr gültig“; keine Anmeldung; frühere Anmeldungen über den Link bleiben bestehen. |
| P-20 | Turnier mit Anmeldungen | Sichtbarkeit öffentlich ↔ privat wechseln | Listung folgt der Sichtbarkeit; Anmeldungen und Live-Zugriff bleiben; neue private Anmeldungen nur mit Link. |
| P-21 | Entwurf mit Freigabelink; Turnier mit Anmeldungen | Über den Link anmelden; zurück auf Entwurf setzen | Keine Anmeldung möglich; Rückkehr zu Entwurf wird mit Anzahl der Anmeldungen abgelehnt. |
| P-22 | Verbinden gelingt serverseitig, Antwort geht verloren | Dasselbe Dokument verbindet erneut | Die Verbindung wird als dieselbe erkannt; kein Konflikt, kein zweiter Lease. |
| P-23 | Zwei lokale Meldungen vor der Verbindung | Verbinden, danach abgleichen | Verbinden ändert nichts; erst der Abgleich legt beide genau einmal online an. |
| P-24 | Online-Kapazität voll | Lokale Nachmeldung abgleichen | Online als bestätigt angelegt, in der Zusammenfassung als „über Kapazität“ ausgewiesen. |
| P-25 | Kein Netz vor der ersten Runde | Erste Runde starten | Hinweis mit Zeitpunkt des letzten Abgleichs; Start erst nach Bestätigung; Bestätigung im Sync-Blatt protokolliert. |
| P-26 | Start ohne Netz; danach geht online eine Anmeldung ein; Netz kommt zurück | Weiterarbeiten, dann abgleichen | `running` wird als erster Schreibvorgang gesetzt; die Anmeldung trägt „nach Turnierstart eingegangen“, wird nicht importiert und im Abgleich gesondert gezeigt; der Hinweis „Turnierstart nicht übertragen“ war bis dahin sichtbar. |
| P-27 | Turnier `running` | Erste Runde in PTM löschen | Online bleibt `running`; neue Anmeldungen bleiben gesperrt. |
| P-28 | Team online angemeldet und vor dem Abgleich lokal zusätzlich erfasst | Abgleichen | Weder Neuanlage noch Import; Anzeige „möglicherweise identisch“; „verknüpfen“ ergibt genau eine Meldung. |
| P-29 | Konto X steht bereits in einem Personen-Slot einer aktiven Anmeldung | Zweite Anmeldung mit X in einem Slot absenden; zusätzlich zwei Gastanmeldungen mit gleichem Namen | Zweite Anmeldung mit X angenommen, beide als Konflikt markiert, X erhält eine Postbox-Nachricht, das absendende Konto sieht keinen Hinweis; Gastanmeldungen angenommen und als mögliche Dublette markiert. |
| P-30 | Dieselbe Benutzer-ID in zwei Teams | Beide einchecken, Runde auslosen | Beide als Konflikt markiert und nicht ausgelost, bis die Turnierleitung auflöst. |
| P-31 | Verbundenes, laufendes Turnier | a) Server liefert 404/403/Timeout, b) Turnier wird gelöscht | a) Verbindung bleibt, Fehler mit nächster Aktion; b) erst `tournament_deleted` beendet die Verbindung und archiviert das Sync-Blatt; die Live-Ansicht zeigt einen Löschhinweis. |
| P-32 | Laufendes Turnier mit Online-Runden, Datei verloren | Übernahme mit neuem Dokument, dann Runde pushen | Warnung „n Online-Runden vorhanden“; Überschreiben nur nach ausdrücklicher Bestätigung. |
| P-33 | Option Check-in-Benachrichtigung aktiv | Einchecken, auschecken, erneut einchecken | Genau eine Nachricht an das verknüpfte Konto; die Live-Ansicht folgt jedem Wechsel. |
| P-34 | Lokale Nachmeldung ohne Konto nach dem Start | Auslosen und Runde pushen | Keine Online-Anmeldung; die Person erscheint nur namentlich in Paarung und Rangliste. |
| P-35 | Person ohne Konto in einem Slot mit Slot-E-Mail; später Konto mit dieser Adresse, danach Konto gelöscht | Konto verifizieren, dann löschen | Verknüpfung erst nach Verifikation und bei Eindeutigkeit, protokolliert; nach dem Löschen bleibt eine Gastanmeldung. |
| P-36 | Slot mit Konto A verknüpft | Turnierleitung korrigiert die Slot-E-Mail auf die Adresse von Konto B | Verknüpfung bleibt bei A; B erhält keinen Live-Zugriff; Audit-Eintrag entsteht. |
| P-37 | Importierte, eingecheckte Meldung | Online stornieren, abgleichen, dann „bewusst behalten“; zweiter Fall: online wieder bestätigen | Markierung „online storniert“ und Ausschluss von der Auslosung; nach „bewusst behalten“ wieder auslosbar ohne neue Online-Anmeldung; nach erneuter Bestätigung entfällt die Markierung. |
| P-38 | Verknüpfte Meldung | a) löschen mit „online stornieren“ ohne Netz, b) „nur lokal entfernen“, c) Zeile manuell löschen; jeweils abgleichen | a) Stornierung wird nachgeholt; b) online unverändert, kein Re-Import; c) Rückfrage statt Storno oder Import. |
| P-39 | Name seit letztem Abgleich nur online / beidseitig unterschiedlich / beidseitig gleich geändert | Abgleich vor und nach `running` | Übernahme nach lokal; Konflikt mit korrekter Vorauswahl ohne Datenänderung; kein Konflikt. |
| P-40 | Verbundenes Dokument 48 Stunden offline | Danach zweites Dokument verbindet; danach Veranstalter löst Bindung online | Erstes Dokument schreibt weiter; zweites erhält nur die Übernahme nach KP-08; nach dem Lösen werden Schreibvorgänge des ersten Dokuments abgewiesen, Audit-Eintrag entsteht. |
| P-41 | Mêlée-Anmeldung, Doublette: 7 eingecheckte Einzelanmeldungen, Teams gebildet; danach 1 weiterer Check-in | Abgleich; dann Mêlée-Übernahme erneut | Der Abgleich bildet keine Teams; das erneute Bilden verwendet nur die noch nicht übernommenen Personen (Teamgrößen nach der vorhandenen Mêlée-Regel); bestehende Teams bleiben unverändert; online bleiben alle 8 Anmeldungen Einzelanmeldungen mit Teamzuordnung. |
| P-42 | Vollständiges, eingechecktes Formée-Team (Triplette) | Eine Person fehlt beim Check-in und wird entfernt | Team wird als unvollständig angezeigt, nicht ausgelost und nicht automatisch aufgelöst; die Online-Besetzung wird beim nächsten Push aktualisiert. |
| P-43 | *(Erweiterung EW-01)* Team A/B online angemeldet (beide mit Konto); vor dem Abgleich lokal mit denselben Anmelde- bzw. Spieler-Codes erfasst | Abgleich | Automatische Verknüpfung, genau eine Anmeldung, Zusammenfassung „1 verknüpft“. |
| P-44 | Person X mit Konto ist online in Team 1 angemeldet | X online zusätzlich in Team 2 eintragen; in einer zweiten Variante in PTM eine gleichnamige Person in ein lokales Team aufnehmen und abgleichen | Online: angenommen, beide Anmeldungen als Konflikt markiert (KP-06 b). Lokal: nur Namenshinweis nach KP-06 c), keine automatische Wirkung. |
| P-45 | Importiertes Team mit Person X (Konto) | In PTM X durch Person Y ersetzen, abgleichen | Online-Slot enthält Y ohne Benutzer-ID; X verliert die Live-Ansicht dieses Teams; die Benutzer-ID von X lässt sich in PTM nicht direkt bearbeiten. |
| P-46 | Slot einer Online-Anmeldung mit Konto A verknüpft | In PTM Online die Kontakt-E-Mail und die Slot-E-Mail auf Adressen von Konto B korrigieren, abgleichen | Verknüpfung bleibt bei A (KP-11); PTM erhält weiter die Benutzer-ID von A. |
| P-47 | Lokale Nachmeldung einer registrierten Person vor `running` | Abgleich, online die Slot-E-Mail eintragen (E-22), erneut abgleichen, Check-in | Nach dem zweiten Abgleich trägt die Meldung in PTM die Benutzer-ID; die Person sieht den Check-in in der Live-Ansicht ihrer Anmeldung. |
| P-48 | *(Erweiterung EW-01)* Online-Team A/B, lokal Team A/C erfasst (A, B, C mit Konto) | Abgleich | Keine automatische Verknüpfung; Hinweis „möglicherweise dasselbe Team“; nach „verknüpfen“ genau eine Anmeldung mit abgeglichener Besetzung. |
| P-49 | Anmeldephase offen | Turnierersteller tauscht in Team A/B online den Partner B gegen D, danach Abgleich | Lokale Besetzung wird übernommen; B verliert, D erhält die Live-Ansicht; Team behält UUID und Online-ID. |
| P-50 | Team eingecheckt | Online und lokal wird die Besetzung unterschiedlich geändert, dann Abgleich | Besetzungskonflikt ohne Datenänderung; lokaler Stand vorausgewählt. |
| P-51 | Triplette | Online Anmeldung mit 1, 2, 3 und 4 Personen absenden | 1 und 4 abgelehnt (Anmeldeeinheit 2 bis 3); 2 als unvollständig angenommen; 3 angenommen. |
| P-52 | Supermêlée, Mêlée-Anmeldung und Tête-à-tête | Anmeldung mit zwei Personen absenden | Jeweils abgelehnt; die Anmeldung ist dort genau eine Person. |
| P-53 | Mêlée-Anmeldung, Check-in abgeschlossen, Teams noch nicht gebildet | Erste Runde starten | Start wird angehalten mit Hinweis „Mêlée-Teams noch nicht gebildet“ und Angebot, den Schritt jetzt auszuführen; es wird nicht verdeckt gebildet. |
| P-54 | Mêlée-Teams gebildet, danach checkt eine weitere Person ein | Erste Runde starten | Vorabcheck nennt die nicht zugeordnete Person; Turnierleitung führt den Schritt erneut nur für offene Personen aus oder startet bewusst ohne sie. |
| P-55 | Kein Netz, Sync nicht pausiert | Drei Check-ins setzen, Dokument speichern, LibreOffice neu starten, Netz wiederherstellen | Alle drei Check-ins werden nach Rückkehr des Netzes übertragen; kein Check-in geht durch den Neustart verloren. |
| P-56 | Doublette-Team A/B, beide Slots mit Konto verknüpft | A und B öffnen die Live-Ansicht vor und nach Check-in; danach wird B online durch D ersetzt | A und B sehen identischen Inhalt derselben Anmeldung; nach dem Wechsel sieht D denselben Inhalt, B hat keinen Zugriff mehr. |
| P-57 | Lokale Nachmeldung vor der Verbindung; danach verbinden und zweimal abgleichen, wobei die Antwort der ersten Online-Anlage verloren geht | Abgleich wiederholen | Die Mapping-Tabelle enthält genau eine Zeile UUID ↔ Online-Anmeldungs-ID; online existiert genau eine Anmeldung (Idempotenzschlüssel UUID). |
| P-58 | Sync pausiert, Online-Anmeldung offen | Erste Runde starten | Vorabcheck weist auf die offene Online-Anmeldung hin; nach Wahl „nur `running` senden“ ist die Online-Anmeldung gesperrt, alle übrigen Übertragungen bleiben pausiert. |
| P-59 | Konto V (z. B. Vereinsvertreter oder Admin) steht in keinem Slot | V meldet drei Teams an | Alle drei Anmeldungen werden angenommen; V erhält dadurch keine Einträge unter „Meine Live-Turniere“ und keine Check-in-Nachricht (ein Admin sieht das Turnier nur über seine Verwaltungsansicht); die Teammitglieder werden über ihre Slot-E-Mails verknüpft. |
| P-60 | Team A/B, abgeschickt von einem Dritten; Slot-E-Mail von B gehört genau einem verifizierten Konto | Anmeldung absenden; danach wählt B „Das bin ich nicht“ | B ist sofort ohne Bestätigung verknüpft und sieht die Live-Ansicht; nach „Das bin ich nicht“ ist der Slot unverknüpft, Name und Anmeldung bleiben, Audit-Eintrag entsteht. |
| P-61 | Team A/B, A mit Konto verknüpft | A versucht online, B durch D zu ersetzen; danach macht es ein Admin | Änderung durch A serverseitig abgelehnt; Änderung durch den Admin angenommen und beim nächsten Abgleich lokal übernommen. |
| P-62 | Mitverwalter M; PTM-Dokument ist mit dem API-Key von M verbunden | Beim Check-in eine Person des Teams lokal austauschen und pushen; in einem zweiten Team ändert M die Besetzung manuell in der Weboberfläche | Beide Änderungen werden angenommen; die manuelle Änderung wird beim nächsten Abgleich lokal übernommen. |
| P-63 | Verbundene Datei wurde kopiert; Original und Kopie schreiben nacheinander | Check-in in der Kopie pushen, danach im Original | Der zweite Schreibvorgang mit veraltetem Zählerstand wird mit `document_forked` abgelehnt; PTM zeigt den Hinweis auf eine Kopie. |
| P-64 | Verbundenes Turnier vor der Veröffentlichung, Testlauf | Erste Runde starten, danach online `running` zurücksetzen; zweiter Fall: mit bereits übertragenem Rundenergebnis zurücksetzen | Warnung vor dem Start; Zurücksetzen ohne Ergebnis möglich und protokolliert, Anmeldung bleibt geschlossen bis zum ausdrücklichen Öffnen; mit Ergebnis abgelehnt. |
| P-65 | Turnier ohne Netz, angesetzter Beginn 10:00 | Um 10:01 online anmelden | Abgelehnt „Anmeldung geschlossen“, obwohl PTM `running` nie übertragen hat. |
| P-66 | Zeilen im Sync-Blatt gelöscht | „Zuordnung vom Server wiederherstellen“, dann abgleichen | Zuordnungen vollständig wiederhergestellt; keine Dubletten, keine falschen Konflikte. |
| P-67 | Mitverwalter übernimmt die Bindung während `running` | Übernahme bestätigen | Turnierersteller erhält eine Postbox-Nachricht; Audit-Eintrag entsteht. |
| P-68 | Konto X, Slot-E-Mail von X wird von einem Fremden in ein Team eingetragen | Absenden; X meldet sich danach selbst in einem anderen Team an | Absender sieht keinen Verknüpfungsstatus; X erhält eine Postbox-Nachricht; die eigene Anmeldung von X wird angenommen, beide Anmeldungen stehen als Konflikt in der Konfliktliste. |
| P-69 | Push eines Check-ins; Antwort geht verloren; LibreOffice wird neu gestartet | Puffer wird erneut übertragen; zusätzlich wird testweise derselbe Auftrag mit veränderter Nutzlast gesendet | Wiederholung mit gleicher Auftrags-ID und gleicher Nutzlast erhält dieselbe Antwort, keine doppelte Wirkung, kein `document_forked`; gleiche Auftrags-ID mit anderer Nutzlast wird mit `idempotency_mismatch` abgelehnt. |
| P-70 | Kapazität voll, Anmeldeschluss überschritten, Turnier nicht `running` | Öffentliche Anmeldung absenden; parallel lokale Nachmeldung abgleichen | Öffentliche Anmeldung abgelehnt bzw. Warteliste; lokale Nachmeldung über den PTM-Zugangsweg angelegt, als `over_capacity` gekennzeichnet, Audit-Eintrag vorhanden. |
| P-71 | Supermêlée, 2. Spieltag mit neuem Online-Turnier verbunden; Stammspieler A online angemeldet, B nur lokal für den Spieltag gemeldet, C spielt nicht, D neu online angemeldet | Abgleichen | A ohne Rückfrage mit seiner Pool-Zeile verknüpft (behält die Spieler-Nr), B online angelegt, C nicht angelegt, D als neue Zeile übernommen. |
| P-72 | Ein Schreibauftrag wird online fachlich abgelehnt (z. B. Besetzung nach Anmeldeschluss gesperrt) | Abgleichen, dann „Zur Kenntnis genommen“ wählen und erneut abgleichen | Der Auftrag steht mit Schreibzähler und Hinweis in der Konfliktliste; danach nicht mehr, die Protokollzeile im Sync-Blatt bleibt (quittiert). |
| P-73 | Online-Anmeldung offen, Turniertag erreicht | Abgleichen, Rückfrage bejahen | Online-Anmeldung geschlossen, Auftrag im Audit-Log mit Rolle „document“; ohne Netz nachgeholt. |


## Abnahmeszenario

**Ausgangslage:** Für das „Herbstpokal Doublette“ sind in PTM Online drei Teams bestätigt. Am Turniertag meldet sich Team Berger/Nguyen zusätzlich am Platz.

1. Die Turnierleitung legt in PTM eine Schweizer-System-Doublette an und verbindet sie mit dem Online-Turnier.

2. Sie wählt **Meldungen abgleichen**. Die drei bestätigten Teams erscheinen in der Meldeliste, jeweils noch ohne Check-in.

3. Sie trägt Team Berger/Nguyen in PTM ein und führt den Abgleich erneut aus. PTM meldet: „0 Meldungen lokal übernommen, 1 online angelegt.“

4. Beim Eintreffen checkt die Turnierleitung die vier anwesenden Teams in PTM ein. Ein bestätigtes, aber nicht erschienenes Team bleibt inaktiv.

5. Vor der ersten Runde meldet PTM keine fehlenden Online-Anmeldungen. Die Auslosung enthält genau die vier eingecheckten Teams.

Damit bleiben öffentliche Anmeldung, lokale Anwesenheit und Turnierauslosung nachvollziehbar getrennt, aber konsistent verbunden.

## Betriebshinweise

- Vor jeder Auslosung Meldeliste und Check-in prüfen; nach eingetragenen Ergebnissen keine Anmeldung oder Teambildung unkontrolliert umsortieren.

- Bei längerer Offline-Phase den Sync pausieren. Nach Wiederherstellung der Verbindung Sync fortsetzen und einen manuellen Abgleich ausführen.

- Eine lokale Trennung bei nicht erreichbarem PTM Online löst die Online-Sperre nicht. Das Online-Turnier bleibt dort gebunden, bis es mit einem PTM-Dokument erneut verbunden und korrekt getrennt wird oder der Veranstalter die Bindung in PTM Online ausdrücklich löst (E-17).

- Wird das Online-Turnier gelöscht, läuft das lokale PTM-Turnier weiter. Das Sync-Blatt bleibt als Archiv; bei Bedarf wird ein anderes Online-Turnier neu verbunden.

## Datenschutz

| ID | Anforderung |
| - | - |
| DS-01 | Wer eine Anmeldung abschickt, muss im Formular bestätigen, dass die eingetragenen Personen mit der Weitergabe ihres Namens und ihrer E-Mail an den Veranstalter einverstanden sind. |
| DS-02 | Automatisch verknüpfte Konten werden per Postbox informiert (E-22). Personen ohne Konto erhalten keine automatische Nachricht; die Information obliegt dem absendenden Konto (DS-01). |
| DS-03 | Namen erscheinen in öffentlichen Teilnehmerlisten, Paarungen und Ranglisten nur, wenn das Turnier seine Teilnehmer öffentlich zeigt; das gilt auch für lokal nachgemeldete Personen. E-Mails, Kontodaten und Tarife sind nie öffentlich (KP-12). |
| DS-04 | Kontakt- und Slot-E-Mails, Antworten auf Online-Fragen und Tarife werden nach Abschluss des Turniers nach einer festen Frist (Standard 12 Monate, für den Veranstalter einstellbar) gelöscht; Namen, Ergebnisse und Ranglisten bleiben erhalten. |
| DS-05 | Das Protokoll nach T-14 bleibt revisionssicher, wird aber nach derselben Frist pseudonymisiert: E-Mail-Adressen und Kontozuordnungen werden durch nicht rückführbare Kennungen ersetzt. |
| DS-06 | Beim Löschen eines Kontos entfällt die Verknüpfung sofort (KP-10). Die vom absendenden Konto eingetragene Slot-E-Mail bleibt bis zur Frist nach DS-04 als Kontaktdatum des Veranstalters erhalten. |

## Umsetzungsstufen

Die Anforderung wird in dieser Reihenfolge umgesetzt; jede Stufe ist für sich abnahmefähig.

| Stufe | Inhalt | Wichtigste Prüffälle |
| - | - | - |
| 1 | Verbinden mit Lease und Schreibzähler, Import bestätigter Anmeldungen, Online-Anlage lokaler Meldungen mit Idempotenz, Check-in-Übertragung, Puffer, Übergang zu `running` inklusive automatischem Anmeldeschluss und Zurücksetzen, Datenbankmigration nach T-20, Wiederherstellung der Zuordnung, Auftrags-Idempotenz (T-23), PTM-Zugangsweg mit `over_capacity` (T-24) | P-01 bis P-15, P-22 bis P-27, P-55, P-57, P-58, P-63 bis P-66, P-69, P-70 |
| 2 | Konfliktliste, Dubletten, Namens- und Besetzungskonflikte, Storno- und Löschpfade, Mêlée-Teamzuordnung | P-28 bis P-30, P-37 bis P-39, P-41, P-42, P-49 bis P-54 |
| 3 | Personen-Slots mit Slot-E-Mail, Kontoverknüpfung, Live-Ansicht der Anmeldung, Check-in-Nachricht, Rollen und Benachrichtigungen, Datenschutz | P-16 bis P-21, P-31 bis P-36, P-44 bis P-47, P-56, P-59 bis P-62, P-67, P-68 |
| Später | EW-01 (Anmelde- und Spieler-Code) mit Vorrang vor EW-02, weil EW-01 den Namensabgleich am Turniertag entlastet | P-43, P-48 |

### Umsetzungsstand (Stand 2026-10-01)

Stufe 1 bis 3 sind umgesetzt, PTM Online und PTM jeweils auf dem Branch `ptm-online-2` (nicht live geschaltet). Bewusste Abweichungen und offene Punkte:

| Punkt | Stand |
| - | - |
| Konfliktliste und Entscheidungen (A-29) | Blatt „PTMOnline Konflikte“ mit Entscheidungsspalte; Entscheidungen wirken beim nächsten Abgleich und werden online protokolliert. |
| Gelöschtes Online-Turnier (KP-07) | Nur 410 bzw. `tournament_deleted` beendet die Verbindung; 404 nie. |
| Supermêlée mehrere Spieltage | Nach E-25 umgesetzt. |
| Auslosung schließt Konfliktmeldungen aus (P-30, P-42) | Umgesetzt: Meldungen mit Konto-Doppelbelegung (bis online aufgelöst oder in der Konfliktliste als „Verschiedene Personen“ freigegeben, KP-06 c) und unvollständige Teams bleiben eingecheckt, werden aber nicht ausgelost; die Turnierleitung sieht, welche Meldungen warum fehlen. Gilt bei verbundenem Dokument für Schweizer, Formule X, Supermêlée, JGJ, Poule und KO; bei Kaskade nur für die Eröffnungsrunde (danach ist die Gruppenstruktur fixiert). Nicht bei Trip-Tête (spielt immer mit allen Meldungen) und bei Mêlée-Anmeldung (Teams entstehen lokal). Die Doppelbelegung kennt PTM aus dem letzten Abgleich bzw. dem Vorabcheck. |
| Nach Turnierstart eingegangene Anmeldung stornieren | Aus PTM nur „Übernehmen“; Stornieren erfolgt in PTM Online. |
| Lokale UUID einer Online-Anmeldung | Das gebundene Dokument darf sie weiter überschreiben (bestehendes PTM-Verhalten). |
| Vorbeugung „Anmeldung schließen“ | Bei Team-Meldelisten zählt jede belegte Aktiv-Spalte als begonnener Check-in; die Rückfrage kann daher etwas früher kommen. |

## Restrisiken und Maßnahmen

| Nr. | Risiko | Maßnahme im Dokument | Restrisiko |
| - | - | - | - |
| R-01 | Kopie der verbundenen Datei schreibt mit | Schreibzähler (E-24, T-19) | Eine Kopie wird erst beim nächsten Schreibversuch erkannt. |
| R-02 | Testlauf sperrt die Anmeldung | Warnung und Zurücksetzen von `running` (E-23) | Nach dem ersten Rundenergebnis nicht mehr umkehrbar. |
| R-03 | Aussperren oder Ausspähen über Slot-E-Mails | Kein Statushinweis, Postbox-Nachricht, Konflikt statt Ablehnung (E-22) | Die Turnierleitung muss Konflikte auflösen. |
| R-04 | Datenschutz bei Daten Dritter | Abschnitt „Datenschutz“ (DS-01 bis DS-06) | Rechtliche Prüfung der Frist und des Einwilligungstexts steht aus. |
| R-05 | Datenverlust durch Migration | T-20 | Keins bei Einhaltung. |
| R-06 | Anmeldung bleibt ohne Netz offen | Automatischer Anmeldeschluss zum Turnierbeginn (E-02) | Anmeldungen zwischen tatsächlichem Start und angesetztem Beginn bleiben möglich und werden nach KP-05 markiert. |
| R-07 | Namensabgleich unter Zeitdruck | Ähnlichkeitshinweis (KP-06 a2), EW-01 priorisiert | Schreibvarianten bleiben bis EW-01 manuell zu prüfen. |
| R-08 | Dialogflut am Turniertag | Gesammelte Konfliktliste (A-29) | – |
| R-09 | Beschädigtes Sync-Blatt | Wiederherstellung vom Server (T-21) | Nie abgeglichene lokale Meldungen sind nicht wiederherstellbar. |
| R-10 | Atomarität in D1 | Bedingte Befehle und Batch, Nebenläufigkeitstests (T-12) | – |
| R-11 | Weitreichende Rechte von Mitverwaltern | Benachrichtigung des Turniererstellers (T-22), Protokoll (T-14) | Die Aktion selbst wird nicht verhindert. |
| R-12 | Bindung ohne Ablauf | Lösen durch Turnierersteller, Mitverwalter oder Admin (KP-17) | – |
| R-13 | Nachmeldungen über Kapazität | Kennzeichnung in der öffentlichen Liste (KP-04) | – |
| R-14 | Umfang und Reihenfolge | Umsetzungsstufen | – |

## Erweiterungen für einen späteren Ausbau

Die folgenden Punkte sind ausdrücklich **nicht Teil dieser Anforderung**. Sie sind vermerkt, damit die jetzige Umsetzung sie nicht verbaut.

### EW-01 Anmelde-Code und Spieler-Code (Kurzcode/QR-Code)

**Ziel:** Die Turnierleitung soll eine Anmeldung oder eine registrierte Person in PTM eindeutig und auch offline erfassen oder einchecken können, ohne Namen abzutippen.

| Element | Beschreibung |
| - | - |
| Anmelde-Code | Jede Online-Anmeldung, ob Einzelperson oder Team, erhält einen eigenen Code, abgeleitet von der Anmelde-ID, als Kurzcode mit Prüfziffer und als QR-Code. Er wird den verknüpften Teammitgliedern und der Turnierleitung angezeigt und ist in der Anmeldebestätigung enthalten. Beim Check-in scannt oder tippt die Turnierleitung ihn und findet so die Meldung mit allen Personen und Benutzer-IDs. |
| Spieler-Code | Jeder registrierte Benutzer hat im Profil einen persönlichen Code, abgeleitet von der Benutzer-ID. Damit kann die Turnierleitung eine registrierte Person bei einer lokalen Nachmeldung oder Besetzungsänderung, auch offline, mit ihrer Benutzer-ID erfassen. |
| Gemeinsame Regeln | Beide Codes enthalten eine Prüfziffer gegen Tippfehler und geben keine weiteren Konto- oder Kontaktdaten preis. Der Anmelde-Code wird mit dem Storno der Anmeldung ungültig. Eine freie Namenssuche über alle Konten ist nicht vorgesehen (Datenschutz). Offline erfasste Codes werden beim nächsten Abgleich serverseitig geprüft; ungültige Codes lassen die Meldung bestehen, aber ohne Kontoverknüpfung und mit Hinweis. |
| Folgewirkungen | Erst mit EW-01 greifen die automatische Verknüpfung über Benutzer-IDs (KP-06 a1/a1'), die sofortige Warnung bei doppelter Benutzer-ID in PTM, und Kontokonflikte durch lokal erfasste Benutzer-IDs. Lokale Nachmeldungen nach `running` erhalten auch mit EW-01 keine Live-Ansicht (KP-13). Die Prüffälle P-43 und P-48 gehören zu EW-01. |
| Vorbereitung jetzt | Die Speicherung der Benutzer-ID pro Person (T-17) und die serverseitige Prüfung empfangener Benutzer-IDs werden schon jetzt so umgesetzt, dass EW-01 ohne Datenmodelländerung ergänzt werden kann. |

### EW-02 Live-Link pro Anmeldung (TODO für später)

**Ziel:** Teammitglieder ohne Benutzerkonto sollen dieselbe Live-Ansicht ihrer Anmeldung sehen können wie die Konten des Teams (E-21).

| Element | Beschreibung |
| - | - |
| Live-Link | Jede Online-Anmeldung erhält einen geheimen, nicht erratbaren Link auf ihre Live-Ansicht. Er zeigt denselben Inhalt wie die kontobasierte Ansicht und nur diese eine Anmeldung. |
| Weitergabe | Die verknüpften Teammitglieder und die Turnierleitung sehen den Link und geben ihn an das Team weiter. PTM Online versendet ihn nicht automatisch. |
| Sicherheit | Gespeichert wird nur ein kryptografischer Hash (wie beim Freigabelink, T-11). Verknüpfte Teammitglieder oder die Turnierleitung können den Link widerrufen und neu erzeugen, z. B. nach einem Besetzungswechsel. Der Link gewährt nur Lesezugriff und keine Änderungen an der Anmeldung. Bei Storno der Anmeldung oder Löschung des Turniers wird er ungültig. |
| Datenschutz | Wie bei der kontobasierten Ansicht werden keine Kontaktdaten, Kontodaten oder Tarife angezeigt (KP-12). |
| Vorbereitung jetzt | Da die Live-Ansicht schon jetzt an der Online-Anmeldungs-ID hängt (T-13), kann der Link später ohne Änderung des Inhaltsmodells ergänzt werden. |
