/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 *******************************************************************************/
package org.caleydo.neo4j.plugins.kshortestpaths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

/**
 * @author Christian
 *
 */
public class EdgePropertyCostEvaluator implements CostEvaluator<Double> {

	// private final Double baseCost;
	// private final Map<String, Double> propertyCosts;
	//
	// private EdgePropertyCostEvaluator(Double baseCost, Map<String, Double> propertyCosts) {
	// this.baseCost = baseCost;
	// this.propertyCosts = propertyCosts;
	// }

	private static final Double FIXED_COST = 1.0;

	private ScriptEngine engine;
	private boolean useFixedCost;
	private Map<Long, Double> costCache = new HashMap<>();

	public EdgePropertyCostEvaluator(String costFunction) {
		if (costFunction == null) {
			useFixedCost = true;
		} else {
			ScriptEngineManager manager = new ScriptEngineManager();
			engine = manager.getEngineByName("JavaScript");

			// JavaScript code in a String. This code defines a script object 'obj'
			// with one method called 'hello'.
			// String script =
			// "var propertyCosts = { size: { big: 2.0, small: 1.0 }, mood: { good: 2.0, bad: 1.0 } }; function getCost(properties) { var totalCost = 1.0; properties.forEach(function (propObject) { var property = propObject[0]; var value = propObject[1]; var propDef = propertyCosts[property]; if (typeof propDef != \"undefined\") { var cost = propDef[value]; if (typeof cost != \"undefined\") { totalCost += cost; } } }); return totalCost; }";
			try {
				engine.eval(costFunction);
			} catch (ScriptException e) {
				e.printStackTrace();
			}
			useFixedCost = false;
		}

	}

	@Override
	public Double getCost(Relationship relationship, Direction direction) {

		if (useFixedCost) {
			return FIXED_COST;
		} else {

			Double c = costCache.get(relationship.getId());
			if (c != null)
				return c;
			// evaluate script
			try {

				// evaluate script

				// javax.script.Invocable is an optional interface.
				// Check whether your script engine implements or not!
				// Note that the JavaScript engine implements Invocable interface.
				Invocable inv = (Invocable) engine;

				// invoke the global function named "hello"

				// List<List<String>> properties = new ArrayList<>();
				// List<String> property1 = new ArrayList<>();
				// property1.add("size");
				// property1.add("big");
				// properties.add(property1);
				// List<String> property2 = new ArrayList<>();
				// property2.add("mood");
				// property2.add("bad");
				// properties.add(property2);

				// String[][] properties = { { "size", "big" }, { "size", "big" } };

				List<Object[]> properties = new ArrayList<Object[]>();

				for (String property : relationship.getPropertyKeys()) {
					properties.add(new Object[] { property, relationship.getProperty(property) });
				}

				Object cost = inv.invokeFunction("getCost", new Object[] { properties.toArray() });

				if (cost instanceof Double) {
					costCache.put(relationship.getId(), (Double) cost);
					return (Double) cost;
				}
			} catch (ScriptException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}

			return FIXED_COST;
		}
	}
}
