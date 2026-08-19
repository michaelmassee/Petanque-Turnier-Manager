package de.petanqueturniermanager.comp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.star.frame.XStatusListener;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;
import com.sun.star.util.URL;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;

/**
 * Reflection-Testhelfer für privaten statischen Zustand von {@link ProtocolHandler} und
 * {@link DocumentPropertiesHelper}, der ohne echtes LibreOffice nicht anders erreichbar ist
 * (siehe {@link ProtocolHandlerStatusPerFrameTest}). Nur für Tests, package-übergreifend public,
 * damit auch Tests in {@code de.petanqueturniermanager} (z.B. für {@code SheetRunner}) darauf
 * zugreifen können.
 */
public final class ProtocolHandlerTestSupport {

	private ProtocolHandlerTestSupport() {
	}

	public static Object getSharedContext() throws Exception {
		return getStaticField(ProtocolHandler.class, "SHARED_CONTEXT");
	}

	public static void setSharedContext(Object value) throws Exception {
		setStaticField(ProtocolHandler.class, "SHARED_CONTEXT", value);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> statusListenersMap() throws Exception {
		return (Map<String, Object>) getStaticField(ProtocolHandler.class, "STATUS_LISTENERS");
	}

	@SuppressWarnings("unchecked")
	public static Map<String, ConcurrentHashMap<String, String>> propListeMap() throws Exception {
		return (Map<String, ConcurrentHashMap<String, String>>) getStaticField(DocumentPropertiesHelper.class, "PROPLISTE");
	}

	/** Baut einen {@code ProtocolHandler.StatusEntry} per Reflection (private record, kein Zugriff sonst möglich). */
	public static Object neuerStatusEntry(XStatusListener listener, URL url, XSpreadsheetDocument dokument)
			throws Exception {
		Class<?> statusEntryClass = Class.forName(ProtocolHandler.class.getName() + "$StatusEntry");
		Constructor<?> ctor = statusEntryClass.getDeclaredConstructor(XStatusListener.class, URL.class,
				XSpreadsheetDocument.class);
		ctor.setAccessible(true);
		return ctor.newInstance(listener, url, dokument);
	}

	/** Belegt {@link DocumentPropertiesHelper}'s OID-Cache direkt, ohne die UNO-Property-Kette zu mocken. */
	public static String seedeTurnierSystem(XSpreadsheetDocument dokument, TurnierSystem turnierSystem)
			throws Exception {
		String oid = UnoRuntime.generateOid(Lo.qi(XInterface.class, dokument));
		ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();
		properties.put(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, String.valueOf(turnierSystem.getId()));
		propListeMap().put(oid, properties);
		return oid;
	}

	/** {@code ProtocolHandler.notifyAllListeners()} ist package-private; als aufrufbares {@link Runnable} kapseln. */
	public static Runnable notifyAllListenersRunnable() throws Exception {
		Method notifyAllListeners = ProtocolHandler.class.getDeclaredMethod("notifyAllListeners");
		notifyAllListeners.setAccessible(true);
		return () -> {
			try {
				notifyAllListeners.invoke(null);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		};
	}

	public static Object getStaticField(Class<?> clazz, String name) throws Exception {
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(null);
	}

	public static void setStaticField(Class<?> clazz, String name, Object value) throws Exception {
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);
		field.set(null, value);
	}
}
