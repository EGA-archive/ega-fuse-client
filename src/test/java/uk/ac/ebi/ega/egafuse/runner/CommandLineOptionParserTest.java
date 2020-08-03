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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import joptsimple.OptionSet;
import uk.ac.ebi.ega.egafuse.model.CliConfigurationValues;

public class CommandLineOptionParserTest {
    private static final String ENABLE = "enable";
    private static final String DISABLE = "disable";
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void cleanTestEnvironment() {
        temporaryFolder.delete();
    }
    
    @Test
    public void parser_WhenGivenCredentialFile_ThenReturnsUsernameAndPassword() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", createCredentialFile(), "-c", "2"};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CliConfigurationValues cliConfigurationValues = CommandLineOptionParser.parser(set);
        assertEquals(cliConfigurationValues.getCredential().getUsername(), "amohan");
        assertEquals(new String(cliConfigurationValues.getCredential().getPassword()), "testpass");
        assertEquals(cliConfigurationValues.getConnection(), 2);
        assertEquals(cliConfigurationValues.getMountPath().toString(), mountFolder.getAbsolutePath().toString());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void parser_WhenGivenCredentialFileMissingPassword_ThenThrowsException() throws IOException{        
        final File mountFolder = new File("tmp/mount");
        final File credFolder = temporaryFolder.newFolder("home", "user");
        final File credFile = new File(credFolder, "credfile.txt");
        try (final FileOutputStream fileOutputStream = new FileOutputStream(credFile)) {
            fileOutputStream.write("username:amohan".getBytes());
            fileOutputStream.flush();
        }
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", credFile.toPath().toAbsolutePath().toString()};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CommandLineOptionParser.parser(set);
    }
    
    @Test(expected = IOException.class)
    public void parser_WhenGivenWrongCredentialFilePath_ThenThrowsException() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", "/randomdir/random.txt"};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CommandLineOptionParser.parser(set);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void parser_WhenGivenMountPathNotExists_ThenThrowsException() throws IOException{
        String[] args = { "-m", "/randomVal", "-cf", createCredentialFile()};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CommandLineOptionParser.parser(set);
    }
    
    @Test
    public void parser_WhenGivenTreeOptionDisable_ThenReturnsFalse() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", createCredentialFile(), "-t", DISABLE};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CliConfigurationValues cliConfigurationValues = CommandLineOptionParser.parser(set);
        assertFalse(cliConfigurationValues.isTreeStructureEnable());
    }
    
    @Test
    public void parser_WhenGivenTreeOptionEnable_ThenReturnsTrue() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", createCredentialFile(), ENABLE};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CliConfigurationValues cliConfigurationValues = CommandLineOptionParser.parser(set);
        assertTrue(cliConfigurationValues.isTreeStructureEnable());
    }
    
    @Test
    public void parser_WhenGivenTreeOptionInvalidInput_ThenReturnsTrue() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", createCredentialFile(), "-t", "invalidinput"};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CliConfigurationValues cliConfigurationValues = CommandLineOptionParser.parser(set);
        assertTrue(cliConfigurationValues.isTreeStructureEnable());
    }
    
    @Test
    public void parser_WhenGivenNocAndNocpf_ThenNoException() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", createCredentialFile()};
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;        
        CliConfigurationValues cliConfigurationValues = CommandLineOptionParser.parser(set);
        assertEquals(cliConfigurationValues.getConnection(), 4);
        assertEquals(cliConfigurationValues.getConnectionPerFile(), 2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void parser_WhenGivencGreaterThancpf_ThenThrowsException() throws IOException{        
        final File mountFolder = temporaryFolder.newFolder("tmp", "mount");
        String[] args = { "-c", "1", "-cpf", "2", "-m", mountFolder.toPath().toAbsolutePath().toString(), "-cf", createCredentialFile() };
        OptionSet set = CommandLineOptionParser.buildParser().parse(args) ;       
        CommandLineOptionParser.parser(set);
    }
    
    private String createCredentialFile() throws IOException {
        final File credFolder = temporaryFolder.newFolder("home", "user");
        final File credFile = new File(credFolder, "credfile.txt");
        try (final FileOutputStream fileOutputStream = new FileOutputStream(credFile)) {
            fileOutputStream.write("username:amohan".getBytes());
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write("password:testpass".getBytes());
            fileOutputStream.flush();
        }
        return credFile.toPath().toAbsolutePath().toString();
    }
}
