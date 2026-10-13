# Nova IPTV – Android S24 Ultra

Früher funktionsfähiger Prototyp (v0.1): zwei lokal gespeicherte M3U-URLs, Senderliste, Suche und Wiedergabe mit Media3 ExoPlayer. Eigene rechtmässige IPTV-Quellen erforderlich. Keine IPTV-Zugangsdaten im Repository speichern.

## APK bauen
In ein neues GitHub-Repository hochladen (inklusive verstecktem Ordner `.github`). Actions → Build Nova IPTV APK → Run workflow. Nach erfolgreichem Build das Artifact `Nova-IPTV-APK` herunterladen, entpacken und `app-debug.apk` auf dem S24 Ultra installieren.

## Noch NICHT implementiert
EPG, Catch-up, VOD, YouTube-Trailer und automatische Xtream-Anmeldung. Diese benötigen weitere Implementierung und kompatible Server-Schnittstellen. Das Projekt ist nicht als fertig getestete App zu verstehen.
