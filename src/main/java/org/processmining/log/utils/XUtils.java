/*
 * Copyright (c) 2014 F. Mannhardt (f.mannhardt@tue.nl)
 * 
 * LICENSE:
 * 
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.processmining.log.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XCostExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContainer;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeID;
import org.deckfour.xes.model.XAttributeList;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XesXmlGZIPSerializer;
import org.deckfour.xes.out.XesXmlSerializer;

import com.google.common.collect.Iterables;

/**
 * Commonly used methods for handling XES logs
 * 
 * @author F. Mannhardt
 *
 */
public class XUtils {

	public static final XEventClassifier STANDARDCLASSIFIER = new XEventAndClassifier(new XEventNameClassifier(),
			new XEventLifeTransClassifier());

	/**
	 * Returns whether the attribute key matches one of the registered standard
	 * extensions of XES.
	 * 
	 * @param attribute
	 * @return whether the attribute is one of the standard extension attributes
	 */
	public static boolean isStandardExtensionAttribute(XAttribute attribute) {
		// Lets hope that the JIT is clever enough to transform this to a hash table
		switch (attribute.getKey()) {
			case XConceptExtension.KEY_NAME :
			case XConceptExtension.KEY_INSTANCE :
			case XTimeExtension.KEY_TIMESTAMP :
			case XLifecycleExtension.KEY_MODEL :
			case XLifecycleExtension.KEY_TRANSITION :
			case XOrganizationalExtension.KEY_GROUP :
			case XOrganizationalExtension.KEY_RESOURCE :
			case XOrganizationalExtension.KEY_ROLE :
			case XCostExtension.KEY_AMOUNT :
			case XCostExtension.KEY_CURRENCY :
			case XCostExtension.KEY_DRIVER :
			case XCostExtension.KEY_TOTAL :
			case XCostExtension.KEY_TYPE :
				return true;
		}
		return false;
	}

	/**
	 * Added by Eric Verbeek
	 * 
	 * Returns a default classifier to use with an event log. If the log
	 * contains classifiers, then the first classifier is returned. Otherwise,
	 * the standard MXML classifier is constructed and returned.
	 * 
	 * @param log
	 * @return A default classifier to use with the provided log.
	 */
	public static XEventClassifier getDefaultClassifier(XLog log) {
		if (log.getClassifiers().isEmpty()) {
			return STANDARDCLASSIFIER;
		}
		return log.getClassifiers().get(0);
	}

	/**
	 * Returns both the event classifiers defined by the XLog, as well as the
	 * three standard classifiers {@link XLogInfoImpl#NAME_CLASSIFIER},
	 * {@link XLogInfoImpl#RESOURCE_CLASSIFIER} and
	 * {@link XLogInfoImpl#STANDARD_CLASSIFIER}.
	 * 
	 * @param log
	 * @return a list of event classifiers that can be used on the log
	 */
	public static List<XEventClassifier> getStandardAndLogDefinedEventClassifiers(XLog log) {
		List<XEventClassifier> classList = new ArrayList<>(log.getClassifiers());
		if (!classList.contains(XLogInfoImpl.RESOURCE_CLASSIFIER)) {
			classList.add(XLogInfoImpl.RESOURCE_CLASSIFIER);
		}
		if (!classList.contains(XLogInfoImpl.STANDARD_CLASSIFIER)) {
			classList.add(XLogInfoImpl.STANDARD_CLASSIFIER);
		}
		if (!classList.contains(XLogInfoImpl.NAME_CLASSIFIER)) {
			classList.add(0, XLogInfoImpl.NAME_CLASSIFIER);
		}
		return classList;
	}

	/**
	 * Returns the event name.
	 * 
	 * @param element
	 * @return the value of the "concept:name" attribute or "null"
	 */
	public static String getConceptName(XAttributable element) {
		return XConceptExtension.instance().extractName(element);
	}

	/**
	 * Returns the event time.
	 * 
	 * @param event
	 * @return the value of the "time:timestamp" attribute or "null"
	 */
	public static Date getTimestamp(XEvent event) {
		return XTimeExtension.instance().extractTimestamp(event);
	}

	public static XLog loadLog(String string) throws UnsupportedEncodingException, Exception {
		return loadLog(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}

	public static XLog loadLog(File file) throws FileNotFoundException, Exception {
		return loadLog(new FileInputStream(file));
	}

	public static XLog loadLog(InputStream is) throws Exception {
		XesXmlParser xmlParser = new XesXmlParser();
		return Iterables.getFirst(xmlParser.parse(is), null);
	}

	public static void saveLog(XLog log, File file) throws FileNotFoundException, IOException {
		saveLogPlain(log, file);
	}

	public static void saveLogPlain(XLog log, File file) throws FileNotFoundException, IOException {
		saveLogWithSerializer(log, file, new XesXmlSerializer());
	}

	public static void saveLogGzip(XLog log, File file) throws FileNotFoundException, IOException {
		saveLogWithSerializer(log, file, new XesXmlGZIPSerializer());
	}

	public static void saveLogWithSerializer(XLog log, File file, XSerializer logSerializer)
			throws FileNotFoundException, IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			logSerializer.serialize(log, out);
		}
	}

	public static boolean containsEventWithName(String eventName, XTrace trace) {
		for (XEvent xEvent : trace) {
			if (eventName.equals(getConceptName(xEvent))) {
				return true;
			}
		}
		return false;
	}

	public static XEvent getLatestEventWithName(String eventName, XTrace trace) {
		XEvent latestEvent = null;
		for (XEvent xEvent : trace) {
			if (eventName.equals(getConceptName(xEvent))) {
				latestEvent = xEvent;
			}
		}
		return latestEvent;
	}

	public static NavigableSet<String> getAllEventNamesSorted(XLog log) {
		NavigableSet<String> eventNames = new TreeSet<>();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				eventNames.add(getConceptName(event));
			}
		}
		return eventNames;
	}

	public static String stringifyEvent(XEvent e, XEventClassifier classifier) {
		return classifier.getClassIdentity(e);
	}

	public static String stringifyEvent(XEvent e) {
		return stringifyEvent(e, new XEventNameClassifier());
	}

	public static String stringifyTrace(XTrace t, XEventClassifier classifier) {
		StringBuilder sBuilder = new StringBuilder("[");
		Iterator<XEvent> iterator = t.iterator();
		while (iterator.hasNext()) {
			sBuilder.append(stringifyEvent(iterator.next(), classifier));
			if (iterator.hasNext()) {
				sBuilder.append(",");
			}
		}
		sBuilder.append("]");
		return sBuilder.toString();
	}

	public static String stringifyTrace(XTrace t) {
		return stringifyTrace(t, new XEventNameClassifier());
	}

	public static String stringifyLog(XLog l, XEventClassifier classifier) {
		StringBuilder sBuilder = new StringBuilder("[");
		Iterator<XTrace> iterator = l.iterator();
		while (iterator.hasNext()) {
			sBuilder.append(stringifyTrace(iterator.next(), classifier));
			if (iterator.hasNext()) {
				sBuilder.append(",\n");
			}
		}
		sBuilder.append("]");
		return sBuilder.toString();
	}

	public static String stringifyLog(XLog l) {
		return stringifyLog(l, new XEventNameClassifier());
	}

	public static String stringifyAttributes(XAttributeMap map) {
		StringBuilder sBuilder = new StringBuilder("{");
		Iterator<XAttribute> iterator = map.values().iterator();
		while (iterator.hasNext()) {
			XAttribute a = iterator.next();
			sBuilder.append(a.getKey() + " -> " + a.toString());
			if (iterator.hasNext()) {
				sBuilder.append(",\n");
			}
		}
		sBuilder.append("}");
		return sBuilder.toString();
	}

	/**
	 * Creates a deep clone of the {@link XAttribute} with the same value, but a
	 * changed key.
	 * 
	 * @param oldAttribute
	 * @param newKey
	 * @return copy of the supplied attribute
	 */
	public static XAttribute cloneAttributeWithChangedKey(XAttribute oldAttribute, String newKey) {
		return cloneAttributeWithChangedKeyWithFactory(oldAttribute, newKey, XFactoryRegistry.instance().currentDefault());
	}

	/**
	 * Creates a deep clone of the {@link XAttribute} with the same value, but a
	 * changed key.
	 * 
	 * @param oldAttribute
	 * @param newKey
	 * @param factory
	 * @return copy of the supplied attribute
	 */
	public static XAttribute cloneAttributeWithChangedKeyWithFactory(XAttribute oldAttribute, String newKey,
			XFactory factory) {
		if (oldAttribute instanceof XAttributeList) {
			XAttributeList newAttribute = factory.createAttributeList(newKey, oldAttribute.getExtension());
			for (XAttribute a : ((XAttributeList) oldAttribute).getCollection()) {
				newAttribute.addToCollection(a);
			}
			return newAttribute;
		} else if (oldAttribute instanceof XAttributeContainer) {
			XAttributeContainer newAttribute = factory.createAttributeContainer(newKey, oldAttribute.getExtension());
			for (XAttribute a : ((XAttributeContainer) oldAttribute).getCollection()) {
				newAttribute.addToCollection(a);
			}
			return newAttribute;
		} else if (oldAttribute instanceof XAttributeLiteral) {
			return factory.createAttributeLiteral(newKey, ((XAttributeLiteral) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeBoolean) {
			return factory.createAttributeBoolean(newKey, ((XAttributeBoolean) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeContinuous) {
			return factory.createAttributeContinuous(newKey, ((XAttributeContinuous) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeDiscrete) {
			return factory.createAttributeDiscrete(newKey, ((XAttributeDiscrete) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeTimestamp) {
			return factory.createAttributeTimestamp(newKey, ((XAttributeTimestamp) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else if (oldAttribute instanceof XAttributeID) {
			return factory.createAttributeID(newKey, ((XAttributeID) oldAttribute).getValue(),
					oldAttribute.getExtension());
		} else {
			throw new IllegalArgumentException("Unexpected attribute type!");
		}
	}

	/**
	 * Creates a deep clone of the supplied event log without classifiers
	 * 
	 * @param log
	 * @return a clone of the supplied log
	 */
	public static XLog cloneLogWithoutClassifier(XLog log) {
		XLog clone = (XLog) log.clone();
		clone.getClassifiers().clear();
		return clone;
	}

	/**
	 * Creates a deep clone of the supplied event log without globals
	 * 
	 * @param log
	 * @return a clone of the supplied log
	 */
	public static XLog cloneLogWithoutGlobals(XLog log) {
		XLog clone = (XLog) log.clone();
		clone.getGlobalTraceAttributes().clear();
		clone.getGlobalEventAttributes().clear();
		return clone;
	}

	/**
	 * Creates a deep clone of the supplied event log without globals and
	 * without classifiers
	 * 
	 * @param log
	 * @return a clone of the supplied log
	 */
	public static XLog cloneLogWithoutGlobalsAndClassifiers(XLog log) {
		XLog clone = (XLog) log.clone();
		clone.getClassifiers().clear();
		clone.getGlobalTraceAttributes().clear();
		clone.getGlobalEventAttributes().clear();
		return clone;
	}

	/**
	 * Returns the value of the {@link XAttribute} as {@link Object}
	 * 
	 * @param attribute
	 * @return value of the attribute
	 */
	public static Object getAttributeValue(XAttribute attribute) {
		if (attribute instanceof XAttributeList) {
			return ((XAttributeList) attribute).getCollection();
		} else if (attribute instanceof XAttributeContainer) {
			return ((XAttributeContainer) attribute).getCollection();
		} else if (attribute instanceof XAttributeLiteral) {
			return ((XAttributeLiteral) attribute).getValue();
		} else if (attribute instanceof XAttributeBoolean) {
			return ((XAttributeBoolean) attribute).getValue();
		} else if (attribute instanceof XAttributeContinuous) {
			return ((XAttributeContinuous) attribute).getValue();
		} else if (attribute instanceof XAttributeDiscrete) {
			return ((XAttributeDiscrete) attribute).getValue();
		} else if (attribute instanceof XAttributeTimestamp) {
			return ((XAttributeTimestamp) attribute).getValue();
		} else if (attribute instanceof XAttributeID) {
			return ((XAttributeID) attribute).getValue();
		} else {
			throw new IllegalArgumentException("Unexpected attribute type!");
		}
	}

	/**
	 * Creates an appropriate {@link XAttribute}, decided on the type of the
	 * parameter atttributeValue.
	 * 
	 * @param attributeName
	 * @param attributeValue
	 * @return
	 */
	public static XAttribute createAttribute(String attributeName, Object attributeValue) {
		return createAttributeWithFactory(attributeName, attributeValue, XFactoryRegistry.instance()
				.currentDefault());
	}

	/**
	 * Creates an appropriate {@link XAttribute}, decided on the type of the
	 * parameter atttributeValue.
	 * 
	 * @param attributeName
	 * @param attributeValue
	 * @param factory
	 * @return
	 */
	public static XAttribute createAttributeWithFactory(String attributeName, Object attributeValue, XFactory factory) {
		return createAttributeWithFactory(attributeName, attributeValue, null, factory);
	}

	/**
	 * Creates an appropriate {@link XAttribute}, deciding by the type of the
	 * parameter atttributeValue.
	 * 
	 * @param attributeName
	 * @param attributeValue
	 * @param extension
	 * @return a {@link XAttribute} with correct type
	 */
	public static XAttribute createAttribute(String attributeName, Object attributeValue, XExtension extension) {
		return createAttributeWithFactory(attributeName, attributeValue, extension, XFactoryRegistry.instance()
				.currentDefault());
	}

	/**
	 * Creates an appropriate {@link XAttribute}, deciding by the type of the
	 * parameter atttributeValue.
	 * 
	 * @param attributeName
	 * @param attributeValue
	 * @param extension
	 * @param factory
	 * @return a {@link XAttribute} with correct type
	 */
	private static XAttribute createAttributeWithFactory(String attributeName, Object attributeValue,
			XExtension extension, XFactory factory) {
		if (attributeValue instanceof Double || attributeValue instanceof Float) {
			return factory.createAttributeContinuous(attributeName, ((Number) attributeValue).doubleValue(), extension);
		} else if (attributeValue instanceof Integer || attributeValue instanceof Long) {
			return factory.createAttributeDiscrete(attributeName, ((Number) attributeValue).longValue(), extension);
		} else if (attributeValue instanceof Date) {
			return factory.createAttributeTimestamp(attributeName, ((Date) attributeValue), extension);
		} else if (attributeValue instanceof Boolean) {
			return factory.createAttributeBoolean(attributeName, ((Boolean) attributeValue), extension);
		} else {
			return factory.createAttributeLiteral(attributeName, attributeValue.toString(), extension);
		}
	}

	/**
	 * Adds multiple {@link XAttribute} to the supplied {@link XAttributable}.
	 * 
	 * @param attributable
	 * @param attributes
	 */
	public static void putAttributes(XAttributable attributable, Iterable<XAttribute> attributes) {
		for (XAttribute a : attributes) {
			putAttribute(attributable, a);
		}
	}

	/**
	 * Adds a single {@link XAttribute} to the supplied {@link XAttributable}.
	 * 
	 * @param attributable
	 * @param attribute
	 */
	public static void putAttribute(XAttributable attributable, XAttribute attribute) {
		attributable.getAttributes().put(attribute.getKey(), attribute);
	}

}