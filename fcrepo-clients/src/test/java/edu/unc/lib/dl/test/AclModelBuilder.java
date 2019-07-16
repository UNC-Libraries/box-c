/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.test;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;

import edu.unc.lib.dl.rdf.CdrAcl;

/**
 * Test utility for constructing models containing access control details using
 * a fluent api.
 *
 * @author bbpennel
 *
 */
public class AclModelBuilder {
    public Model model;
    private Resource resc;

    public AclModelBuilder(String title) {
        model = createDefaultModel();
        resc = model.getResource("");
        resc.addProperty(DC.title, title);
    }

    public AclModelBuilder addUnitOwner(String princ) {
        return addProp(CdrAcl.unitOwner, princ);
    }

    public AclModelBuilder addCanManage(String princ) {
        return addProp(CdrAcl.canManage, princ);
    }

    public AclModelBuilder addCanIngest(String princ) {
        return addProp(CdrAcl.canIngest, princ);
    }

    public AclModelBuilder addCanAccess(String princ) {
        return addProp(CdrAcl.canAccess, princ);
    }

    public AclModelBuilder addCanViewOriginals(String princ) {
        return addProp(CdrAcl.canViewOriginals, princ);
    }

    public AclModelBuilder addPatronAccess(String value) {
        return addProp(CdrAcl.patronAccess, value);
    }

    private AclModelBuilder addProp(Property prop, String princ) {
        resc.addProperty(prop, princ);
        return this;
    }
}
