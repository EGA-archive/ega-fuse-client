/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ega.egafuse.runner;

import java.io.IOException;

import org.springframework.core.env.PropertySource;

import joptsimple.OptionSet;
import uk.ac.ebi.ega.egafuse.model.CliConfigurationValues;

public final class CommandLineOptionPropertySource extends PropertySource<OptionSet> {

    private CliConfigurationValues cliConfigurationValues;

    public CommandLineOptionPropertySource(String name, OptionSet options) throws IOException {
        super(name, options);
        this.cliConfigurationValues = CommandLineOptionParser.parser(options);
    }

    @Override
    public Object getProperty(String name) {
        switch (name) {
        case "cred.username":
            return cliConfigurationValues.getCredential().getUsername();
        case "cred.password":
            return cliConfigurationValues.getCredential().getPassword();
        case "maxCache":
            return String.valueOf(cliConfigurationValues.getMaxCache());
        case "connection":
            return String.valueOf(cliConfigurationValues.getConnection());
        case "connectionPerFile":
            return String.valueOf(cliConfigurationValues.getConnectionPerFile());            
        case "mountPath":
            return cliConfigurationValues.getMountPath().toString();
        case "tree":
            return cliConfigurationValues.isTreeStructureEnable();            
        default:
            return null;
        }
    }
}
