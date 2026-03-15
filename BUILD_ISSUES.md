# Build-Probleme und Lösungen


## Problem 2: IDL-Interfaces fehlen (XGlobal etc.)

**Symptom:** Kompilierungsfehler: `Package de.petanqueturniermanager.addin ist nicht vorhanden`

**Ursache:** Die IDL-Dateien (`idl/de/petanqueturniermanager/*.idl`) müssen zu Java-Interfaces kompiliert werden. Dies geschah automatisch im alten Eclipse UNO Plugin-Workflow, ist aber in Gradle nicht konfiguriert.

**Temporäre Lösung:**

1. Verwende das alte Eclipse-Build-System einmal, um die Java-Interfaces zu generieren
2. Kopiere die generierten Dateien ins `src/main/java/de/petanqueturniermanager/addin/` Verzeichnis

**Dauerhafte Lösung (empfohlen):**

Modernisiere das Projekt wie das Referenzprojekt ohne IDL:
- Entferne IDL-Abhängigkeit
- Implementiere UNO-Services direkt in Java
- Nutze moderne .components-Registrierung (bereits vorhanden)


### Manueller Build (falls Script nicht funktioniert):
```bash
# Global init.gradle temporär deaktivieren
mv ~/.gradle/init.gradle ~/.gradle/init.gradle.backup


# Init.gradle wiederherstellen 
mv ~/.gradle/init.gradle.backup ~/.gradle/init.gradle
```



## Änderungen gegenüber Original

1. ✅ JCenter entfernt (deprecated in Gradle 9.x)
2. ✅ Feste Dependency-Versionen statt "+"
3. ✅ Moderne .components-Registrierung
4. ✅ META-INF/manifest.xml erstellt
5. ✅ Veraltete Eclipse-UNO-Dateien entfernt (types.rdb, .unoproject, etc.)
6. ✅ .gitignore aktualisiert
7. ⚠️  IDL-Kompilierung fehlt noch (muss manuell erfolgen)

## Nächste Schritte

1. IDL-Java-Interfaces manuell generieren ODER
2. Projekt auf moderne IDL-freie Implementierung umstellen (wie Referenzprojekt)
