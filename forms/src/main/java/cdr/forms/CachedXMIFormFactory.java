package cdr.forms;

import gov.loc.mods.mods.MODSPackage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import crosswalk.CrosswalkPackage;
import crosswalk.Editable;
import crosswalk.EditingContainer;
import crosswalk.Form;

public class CachedXMIFormFactory extends AbstractFormFactory {
	private static final Logger LOG = LoggerFactory.getLogger(CachedXMIFormFactory.class);
	
	ResourceSet rs = null;
	Map<String, Form> cache = new HashMap<String, Form>();
	String formPath = null;
	CrosswalkPackage crosswalkPackage = null;
	MODSPackage modsPackage = null;
	NotationPackage notationPackage = null;

	public CachedXMIFormFactory() {
		rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("form", new XMIResourceFactoryImpl());
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMLResourceFactoryImpl());
		crosswalkPackage = CrosswalkPackage.eINSTANCE;
		modsPackage = MODSPackage.eINSTANCE;
		notationPackage = NotationPackage.eINSTANCE;
	}

	public String getFormPath() {
		return formPath;
	}

	public void setFormPath(String formPath) {
		this.formPath = formPath;
	}

	@Override
	public Form getForm(String id) {
		if (true || !cache.containsKey(id)) {
			loadForm(id);
		}
		return cache.get(id);
	}

	private void loadForm(String id) {
		File f = new File(getFormPath(), id + ".form");
		if (f.exists()) {
			URI formURI = URI.createFileURI(f.getPath());
			Resource formResource = rs.getResource(formURI, true);
			EObject con = formResource.getContents().get(0);
			if (con instanceof EditingContainer) {
				Editable model = ((EditingContainer) con).getModel();
				if (model instanceof Form) {
					cache.put(id, (Form) model);
				}
			}
		} else {
			LOG.error("Cannot load form: "+f.getPath());
		}
	}

}
