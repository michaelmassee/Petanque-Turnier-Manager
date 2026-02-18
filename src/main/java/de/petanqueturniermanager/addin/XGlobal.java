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
    };

    String ptmstringproperty(String propname);

    int ptmintproperty(String propname);

    String ptmturniersystem();

    int ptmdirektvergleich(int teamA, int teamB, int[][] begegnungen, int[][] siege, int[][] spielpunkte);
}
