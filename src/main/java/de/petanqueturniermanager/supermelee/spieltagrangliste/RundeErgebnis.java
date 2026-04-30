package de.petanqueturniermanager.supermelee.spieltagrangliste;

record RundeErgebnis(int plus, int minus, boolean gespielt) {

    static RundeErgebnis nichtGespielt(int defaultPlus, int defaultMinus) {
        return new RundeErgebnis(defaultPlus, defaultMinus, false);
    }

    static RundeErgebnis gespielt(int plus, int minus) {
        return new RundeErgebnis(plus, minus, true);
    }
}
