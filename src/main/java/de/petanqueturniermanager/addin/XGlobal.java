package de.petanqueturniermanager.addin;

import com.sun.star.lib.uno.typeinfo.MethodTypeInfo;
import com.sun.star.lib.uno.typeinfo.TypeInfo;
import com.sun.star.uno.XInterface;

/**
 * UNO Interface for GlobalAddIn service providing Petanque Tournament Manager Calc functions.
 * Based on IDL: idl/de/petanqueturniermanager/AddInGlobal.idl
 *
 * UNOTYPEINFO is required so the UNO bridge can resolve method signatures
 * without a compiled .rdb type library.
 */
public interface XGlobal extends XInterface {

    // UNO Type Info - method indices must match IDL order
    TypeInfo[] UNOTYPEINFO = {
        new MethodTypeInfo("ptmstringproperty", 0, 0),
        new MethodTypeInfo("ptmintproperty", 1, 0),
        new MethodTypeInfo("ptmturniersystem", 2, 0),
        new MethodTypeInfo("ptmdirektvergleich", 3, 0),
        new MethodTypeInfo("ptmaktuellerunde", 4, 0),
        new MethodTypeInfo("ptmaktuellerspieltag", 5, 0),
        new MethodTypeInfo("ptmoperationaktiv", 6, 0),
        new MethodTypeInfo("ptmsmtriplanzdoublette",  7, 0),
        new MethodTypeInfo("ptmsmtriplanztriplette",  8, 0),
        new MethodTypeInfo("ptmsmnurdoublette",       9, 0),
        new MethodTypeInfo("ptmsmdouplanzdoublette", 10, 0),
        new MethodTypeInfo("ptgsmdouplanztriplette", 11, 0),
        new MethodTypeInfo("ptgsmnurtriplette",                 12, 0),
        new MethodTypeInfo("ptmsmtriplanzpaarungen",            13, 0),
        new MethodTypeInfo("ptmsmtriplanzbahnen",               14, 0),
        new MethodTypeInfo("ptmsmdouplanzpaarungen",            15, 0),
        new MethodTypeInfo("ptmsmdoupanzbahnen",                16, 0),
        new MethodTypeInfo("ptmsmvalide",                       17, 0),
        new MethodTypeInfo("ptmsmanztriplwennnurtriplette",     18, 0),
        new MethodTypeInfo("ptmsmanzdoublwennnurdoublette",     19, 0),
        new MethodTypeInfo("ptmcadrageanzteams",                20, 0),
        new MethodTypeInfo("ptmcadragezielanz",                 21, 0),
        new MethodTypeInfo("ptmcadrageanzfreilose",             22, 0),
        new MethodTypeInfo("ptmpouleanzgruppen",                23, 0),
        new MethodTypeInfo("ptmpouleanzvierergruppen",          24, 0),
        new MethodTypeInfo("ptmpouleanzdreiergruppen",          25, 0),
        new MethodTypeInfo("ptmbooleanproperty",                26, 0),
    };

    String ptmstringproperty(String propname);

    int ptmintproperty(String propname);

    String ptmturniersystem();

    int ptmdirektvergleich(int teamA, int teamB, int[][] begegnungen, int[][] siege, int[][] spielpunkte);

    int ptmaktuellerunde();

    int ptmaktuellerspieltag();

    int ptmoperationaktiv();

    int ptmsmtriplanzdoublette(int anzSpieler);

    int ptmsmtriplanztriplette(int anzSpieler);

    int ptmsmnurdoublette(int anzSpieler);

    int ptmsmdouplanzdoublette(int anzSpieler);

    int ptgsmdouplanztriplette(int anzSpieler);

    int ptgsmnurtriplette(int anzSpieler);

    int ptmsmtriplanzpaarungen(int anzSpieler);

    int ptmsmtriplanzbahnen(int anzSpieler);

    int ptmsmdouplanzpaarungen(int anzSpieler);

    int ptmsmdoupanzbahnen(int anzSpieler);

    int ptmsmvalide(int anzSpieler);

    int ptmsmanztriplwennnurtriplette(int anzSpieler);

    int ptmsmanzdoublwennnurdoublette(int anzSpieler);

    int ptmcadrageanzteams(int anzTeams);

    int ptmcadragezielanz(int anzTeams);

    int ptmcadrageanzfreilose(int anzTeams);

    int ptmpouleanzgruppen(int anzTeams);

    int ptmpouleanzvierergruppen(int anzTeams);

    int ptmpouleanzdreiergruppen(int anzTeams);

    long ptmbooleanproperty(String propname);
}
