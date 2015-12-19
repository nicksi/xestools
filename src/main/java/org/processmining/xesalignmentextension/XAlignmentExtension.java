/*
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
package org.processmining.xesalignmentextension;

import java.net.URI;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.XExtensionManager;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.log.utils.XUtils;

import com.google.common.collect.AbstractIterator;
import com.google.common.primitives.Doubles;

/**
 * This extension allows to store an event log that has been aligned to some
 * model.
 * 
 * It defines nine attributes:
 * <ul>
 * <li>alignment:modelid -</li>
 * <li>alignment:fitness</li>
 * <li>alignment:penaltycost - Not yet implementet</li>
 * <li>alignment:likelihoodcost - Not yet implemented</li>
 * <li>alignment:movetype - </li>
 * <li>alignment:modelmove - label of the activity (might be ambiguous)</li>
 * <li>alignment:logmove - label of the event (might be ambiguous)</li>
 * <li>alignment:activityid - unique id of the activity</li>
 * <li>alignment:eventclassid - unique class of the event</li>
 * <li>alignment:observable -</li>
 * </ul>
 * 
 * @author F. Mannhardt (f.mannhardt@tue.nl)
 *
 */
public final class XAlignmentExtension extends XExtension {

	public enum MoveType {
		SYNCHRONOUS, LOG, MODEL
	}

	private static final long serialVersionUID = -3134691538906424946L;

	/**
	 * Unique URI of this extension.
	 */
	public static final URI EXTENSION_URI = URI.create("http://www.xes-standard.org/alignment.xesext");

	/**
	 * Keys for the attributes.
	 */

	public static final String KEY_FITNESS = "alignment:fitness";
	public static final String KEY_PENALTY_COST = "alignment:penaltycost";
	public static final String KEY_LIKELIHOOD_COST = "alignment:likelihoodcost";

	public static final String KEY_MOVE_TYPE = "alignment:movetype";
	public static final String KEY_MOVE_MODEL = "alignment:modelmove";
	public static final String KEY_MOVE_LOG = "alignment:logmove";
	public static final String KEY_ACTIVITY_ID = "alignment:activityid";
	public static final String KEY_EVENTCLASS_ID = "alignment:eventclassid";
	public static final String KEY_MOVE_OBSERVABLE = "alignment:observable";

	public static XAttribute ATTR_FITNESS;
	public static XAttributeDiscrete ATTR_PENALTY_COST;
	public static XAttributeDiscrete ATTR_LIKELIHOOD_COST;

	public static XAttributeDiscrete ATTR_MOVE_TYPE;
	public static XAttributeLiteral ATTR_MOVE_MODEL;
	public static XAttributeLiteral ATTR_MOVE_LOG;
	public static XAttributeLiteral ATTR_ACTIVITY_ID;
	public static XAttributeLiteral ATTR_EVENTCLASS_ID;
	public static XAttributeBoolean ATTR_MOVE_OBSERVABLE;

	/**
	 * Singleton instance of this extension.
	 */
	private final static XAlignmentExtension SINGLETON;

	private XFactory factory = XFactoryRegistry.instance().currentDefault();

	static {
		SINGLETON = new XAlignmentExtension();
		XExtensionManager.instance().register(SINGLETON);
	}

	/**
	 * @return the singleton {@link XAlignmentExtension}.
	 */
	public static XAlignmentExtension instance() {
		return SINGLETON;
	}

	private Object readResolve() {
		return SINGLETON;
	}

	private XAlignmentExtension() {
		super("Alignment", "alignment", EXTENSION_URI);

		ATTR_FITNESS = factory.createAttributeContinuous(KEY_FITNESS, 0.0, this);
		ATTR_PENALTY_COST = factory.createAttributeDiscrete(KEY_PENALTY_COST, 0, this);
		ATTR_LIKELIHOOD_COST = factory.createAttributeDiscrete(KEY_LIKELIHOOD_COST, 0, this);

		ATTR_MOVE_TYPE = factory.createAttributeDiscrete(KEY_MOVE_TYPE, -1, this);
		ATTR_MOVE_MODEL = factory.createAttributeLiteral(KEY_MOVE_MODEL, "__INVALID__", this);
		ATTR_MOVE_LOG = factory.createAttributeLiteral(KEY_MOVE_LOG, "__INVALID__", this);
		ATTR_ACTIVITY_ID = factory.createAttributeLiteral(KEY_ACTIVITY_ID, "__INVALID__", this);
		ATTR_EVENTCLASS_ID = factory.createAttributeLiteral(KEY_EVENTCLASS_ID, "__INVALID__", this);

		ATTR_MOVE_OBSERVABLE = factory.createAttributeBoolean(KEY_MOVE_OBSERVABLE, false, this);

		this.traceAttributes.add((XAttribute) ATTR_FITNESS.clone());
		this.traceAttributes.add((XAttribute) ATTR_PENALTY_COST.clone());
		this.traceAttributes.add((XAttribute) ATTR_LIKELIHOOD_COST.clone());

		this.eventAttributes.add((XAttribute) ATTR_PENALTY_COST.clone());
		this.eventAttributes.add((XAttribute) ATTR_LIKELIHOOD_COST.clone());

		this.eventAttributes.add((XAttribute) ATTR_MOVE_TYPE.clone());
		this.eventAttributes.add((XAttribute) ATTR_MOVE_MODEL.clone());
		this.eventAttributes.add((XAttribute) ATTR_MOVE_LOG.clone());
		this.eventAttributes.add((XAttribute) ATTR_ACTIVITY_ID.clone());
		this.eventAttributes.add((XAttribute) ATTR_EVENTCLASS_ID.clone());

		// Not required for compliance
		//this.eventAttributes.add((XAttribute) ATTR_MOVE_OBSERVABLE.clone());
	}
	
	public XFactory getFactory() {
		return factory;
	}

	public void setFactory(XFactory factory) {
		this.factory = factory;
	}

	public void assignFitness(XLog log, double fitness) {
		internalAssignFitness(log, fitness);
	}

	public void assignFitness(XTrace trace, double fitness) {
		internalAssignFitness(trace, fitness);
	}

	private void internalAssignFitness(XAttributable a, double fitness) {
		XAttributeContinuous attr = (XAttributeContinuous) ATTR_FITNESS.clone();
		attr.setValue(fitness);
		a.getAttributes().put(attr.getKey(), attr);
	}

	public Double extractFitness(XLog log) {
		return internalExtractFitness(log);
	}

	public Double extractFitness(XTrace trace) {
		return internalExtractFitness(trace);
	}

	private Double internalExtractFitness(XAttributable a) {
		XAttribute fitness = a.getAttributes().get(KEY_FITNESS);
		if (fitness != null) {
			return ((XAttributeContinuous) fitness).getValue();
		}
		return null;
	}

	public void assignMoveType(XEvent event, MoveType moveType) {
		XUtils.putAttribute(event, factory.createAttributeDiscrete(KEY_MOVE_TYPE, moveType.ordinal(), this));
	}

	public void assignModelMove(XEvent event, String modelMove) {
		XUtils.putAttribute(event, factory.createAttributeLiteral(KEY_MOVE_MODEL, modelMove, this));
	}

	public void assignLogMove(XEvent event, String logMove) {
		XUtils.putAttribute(event, factory.createAttributeLiteral(KEY_MOVE_LOG, logMove, this));
	}

	public void assignActivityId(XEvent event, String activityId) {
		XUtils.putAttribute(event, factory.createAttributeLiteral(KEY_ACTIVITY_ID, activityId, this));
	}

	public void assignEventClassId(XEvent event, String eventClassId) {
		XUtils.putAttribute(event, factory.createAttributeLiteral(KEY_EVENTCLASS_ID, eventClassId, this));
	}

	protected String extractActivityId(XEvent event) {
		XAttribute value = event.getAttributes().get(KEY_ACTIVITY_ID);
		if (value != null) {
			return ((XAttributeLiteral) value).getValue();
		}
		return null;
	}

	protected String extractEventClassId(XEvent event) {
		XAttribute value = event.getAttributes().get(KEY_EVENTCLASS_ID);
		if (value != null) {
			return ((XAttributeLiteral) value).getValue();
		}
		return null;
	}

	protected String extractLogMove(XEvent event) {
		XAttribute value = event.getAttributes().get(KEY_MOVE_LOG);
		if (value != null) {
			return ((XAttributeLiteral) value).getValue();
		}
		return null;
	}

	protected String extractModelMove(XEvent event) {
		XAttribute value = event.getAttributes().get(KEY_MOVE_MODEL);
		if (value != null) {
			return ((XAttributeLiteral) value).getValue();
		}
		return null;
	}

	protected MoveType extractMoveType(XEvent event) {
		XAttribute value = event.getAttributes().get(KEY_MOVE_TYPE);
		if (value != null && value instanceof XAttributeDiscrete) {
			return MoveType.values()[(int) (((XAttributeDiscrete) value).getValue())];
		} else if (value != null) {
			return MoveType.valueOf(((XAttributeLiteral) value).getValue());
		}
		return null;
	}

	public void assignIsObservable(XEvent event, Boolean observable) {
		XAttributeBoolean attr = (XAttributeBoolean) ATTR_MOVE_OBSERVABLE.clone();
		attr.setValue(observable);
		event.getAttributes().put(attr.getKey(), attr);
	}

	public Boolean extractIsObservable(XEvent event) {
		XAttribute value = event.getAttributes().get(KEY_MOVE_OBSERVABLE);
		if (value != null) {
			return ((XAttributeBoolean) value).getValue();
		}
		return null;
	}

	/**
	 * Wrapper for an {@link XEvent} providing easy access to the information
	 * stored in the {@link XAlignmentExtension}.
	 * 
	 * @author F. Mannhardt
	 *
	 */
	public interface XAlignmentMove {

		MoveType getType();

		String getModelMove();

		String getActivityId();

		String getLogMove();

		String getEventClassId();

		boolean isObservable();

		XEvent getEvent();
	}

	private final static class XAlignmentMoveIterator extends AbstractIterator<XAlignmentMove> {

		private final Iterator<XEvent> traceIter;

		public XAlignmentMoveIterator(XTrace trace) {
			this.traceIter = trace.iterator();
		}

		protected XAlignmentMove computeNext() {
			if (traceIter.hasNext()) {
				final XEvent event = traceIter.next();
				return new XAlignmentMoveImplementation(event);
			} else {
				return endOfData();
			}
		}

	}

	private static final class XAlignmentMoveImplementation implements XAlignmentMove {

		private final XEvent event;

		private XAlignmentMoveImplementation(XEvent event) {
			this.event = event;
		}

		public MoveType getType() {
			return SINGLETON.extractMoveType(event);
		}

		public String getModelMove() {
			return SINGLETON.extractModelMove(event);
		}

		public String getLogMove() {
			return SINGLETON.extractLogMove(event);
		}

		public String getEventClassId() {
			return SINGLETON.extractEventClassId(event);
		}

		public String getActivityId() {
			return SINGLETON.extractActivityId(event);
		}

		public boolean isObservable() {
			Boolean isObservable = SINGLETON.extractIsObservable(event);
			return isObservable == null ? true : isObservable;
		}

		public XEvent getEvent() {
			return event;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((event == null) ? 0 : event.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			XAlignmentMoveImplementation other = (XAlignmentMoveImplementation) obj;
			if (event == null) {
				if (other.event != null)
					return false;
			} else if (!event.equals(other.event))
				return false;
			return true;
		}

	}

	/**
	 * Wraps the {@link XEvent} in an {@link XAlignmentMove} for easier access
	 * to alignment information.
	 * 
	 * @param event
	 * @return
	 */
	public XAlignmentMove extendEvent(XEvent event) {
		return new XAlignmentMoveImplementation(event);
	}

	/**
	 * @param trace
	 * @return an {@link Iterator} wrapping each event in an
	 *         {@link XAlignmentMove}
	 */
	public Iterator<XAlignmentMove> moveIterator(XTrace trace) {
		return new XAlignmentMoveIterator(trace);
	}

	/**
	 * Wrapper for an {@link XTrace} providing easy access to information stored
	 * using {@link XAlignmentExtension}.
	 * 
	 * @author F. Mannhardt
	 *
	 */
	public interface XAlignment extends List<XAlignmentMove> {

		/**
		 * @return
		 */
		double getFitness();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.List#size()
		 */
		int size();

		/**
		 * @return
		 */
		String getName();

		/**
		 * @return
		 */
		XTrace getTrace();

	}

	private final static class XAlignmentIterator extends AbstractIterator<XAlignment> {

		private final Iterator<XTrace> logIter;

		public XAlignmentIterator(XLog log) {
			this.logIter = log.iterator();
		}

		protected XAlignment computeNext() {
			if (logIter.hasNext()) {
				final XTrace trace = logIter.next();
				return new XAlignmentImplementation(trace);
			} else {
				return endOfData();
			}
		}

	}

	private static final class XAlignmentImplementation extends AbstractList<XAlignmentMove> implements XAlignment {

		private final XTrace trace;

		private XAlignmentImplementation(XTrace trace) {
			this.trace = trace;
		}

		public String getName() {
			return XUtils.getConceptName(trace);
		}

		public double getFitness() {
			return XAlignmentExtension.instance().extractFitness(trace);
		}

		public int size() {
			return trace.size();
		}

		public XTrace getTrace() {
			return trace;
		}

		public XAlignmentMove get(int index) {
			return new XAlignmentMoveImplementation(trace.get(index));
		}

		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
			result = prime * result + (Doubles.hashCode(getFitness()));
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			XAlignmentImplementation other = (XAlignmentImplementation) obj;
			if (getName() == null) {
				if (other.getName() != null)
					return false;
			} else if (!getName().equals(other.getName()))
				return false;
			if (Double.compare(getFitness(), other.getFitness()) != 0)
				return false;
			return true;
		}

	}

	/**
	 * Wraps the trace in an {@link XAlignment} for easier access to stored
	 * alignment information.
	 * 
	 * @param trace
	 * @return
	 */
	public XAlignment extendTrace(XTrace trace) {
		return new XAlignmentImplementation(trace);
	}

	/**
	 * Wrapper for an {@link XTrace} providing easy access to information stored
	 * using {@link XAlignmentExtension}.
	 * 
	 * @author F. Mannhardt
	 *
	 */
	public interface XAlignedLog extends List<XAlignment> {

		/**
		 * @return the underlying {@link XLog}
		 */
		XLog getLog();

		/**
		 * @return average fitness of all aligned traces
		 */
		double getAverageFitness();

		/* (non-Javadoc)
		 * @see java.util.List#size()
		 */
		int size();

	}

	private final class XAlignedLogImplementation extends AbstractList<XAlignment> implements XAlignedLog {

		private final XLog log;

		public XAlignedLogImplementation(XLog log) {
			this.log = log;
		}

		public int size() {
			return log.size();
		}

		public double getAverageFitness() {
			return XAlignmentExtension.this.extractFitness(log);
		}

		public XAlignment get(int index) {
			return new XAlignmentImplementation(log.get(index));
		}

		public XLog getLog() {
			return log;
		}

	}

	/**
	 * @param log
	 * @return an {@link Iterator} wrapping each {@link XTrace} in
	 *         {@link XAlignment}.
	 */
	public Iterator<XAlignment> alignmentIterator(XLog log) {
		return new XAlignmentIterator(log);
	}

	/**
	 * Wraps the {@link XLog} into a {@link XAlignedLog} for easier access of
	 * alignment related information.
	 * 
	 * @param log
	 * @return
	 */
	public XAlignedLog extendLog(XLog log) {
		return new XAlignedLogImplementation(log);
	}

}
