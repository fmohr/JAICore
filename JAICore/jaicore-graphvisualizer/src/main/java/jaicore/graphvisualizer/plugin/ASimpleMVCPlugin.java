package jaicore.graphvisualizer.plugin;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaicore.graphvisualizer.events.graph.bus.AlgorithmEventSource;
import jaicore.graphvisualizer.events.gui.GUIEventSource;

public abstract class ASimpleMVCPlugin<M extends ASimpleMVCPluginModel<V, C>, V extends ASimpleMVCPluginView<M, C>, C extends ASimpleMVCPluginController<M, V>> implements IGUIPlugin {

	private final Logger logger = LoggerFactory.getLogger(ASimpleMVCPlugin.class);
	private final M model;
	private final V view;
	private final C controller;

	public ASimpleMVCPlugin() {
		super();
		Type[] mvcPatternClasses = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
		M model;
		V view;
		C controller;
		try {
			System.out.println(mvcPatternClasses[0].getTypeName().replaceAll("(<.*>)", ""));
			Class<M> modelClass = ((Class<M>) Class.forName(mvcPatternClasses[0].getTypeName().replaceAll("(<.*>)", "")));
			Class<V> viewClass = ((Class<V>) Class.forName(mvcPatternClasses[1].getTypeName().replaceAll("(<.*>)", "")));
			Class<C> controllerClass = ((Class<C>) Class.forName(mvcPatternClasses[2].getTypeName().replaceAll("(<.*>)", "")));
			model = modelClass.newInstance();
			view = viewClass.getDeclaredConstructor(modelClass).newInstance(model);
			controller = controllerClass.getDeclaredConstructor(modelClass, viewClass).newInstance(model, view);
			controller.start();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Could not initialize {} due to exception in building MVC.", this);
			this.model = null;
			this.view = null;
			this.controller = null;
			return;
		}
		this.model = model;
		this.view = view;
		this.controller = controller;
		this.model.setController(controller);
		this.model.setView(view);
		this.view.setController(controller);
	}

	@Override
	public IGUIPluginController getController() {
		return controller;
	}

	@Override
	public IGUIPluginModel getModel() {
		return view.getModel();
	}

	@Override
	public IGUIPluginView getView() {
		return view;
	}

	@Override
	public void setAlgorithmEventSource(AlgorithmEventSource graphEventSource) {
		graphEventSource.registerListener(controller);
	}

	@Override
	public void setGUIEventSource(GUIEventSource guiEventSource) {
		guiEventSource.registerListener(controller);
	}
}
