/*
 * Copyright 2016 ELIXIR EGA
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
 */

package eu.elixir.ega.ebi.egafuse;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author asenf
 */
public final class Glue {

    private static Glue instance;

    private Glue() {
        // Nothing to do here
    }

    public synchronized byte[] GenerateRandomString(int minLength, int maxLength, int minLCaseCount, int minUCaseCount, int minNumCount, int minSpecialCount) {
        char[] randomString;

        String LCaseChars = "abcdefgijkmnopqrstwxyz";
        String UCaseChars = "ABCDEFGHJKLMNPQRSTWXYZ";
        String NumericChars = "23456789";
        String SpecialChars = "*$-+?_&=!%{}/";

        HashMap charGroupsUsed = new HashMap();
        charGroupsUsed.put("lcase", minLCaseCount);
        charGroupsUsed.put("ucase", minUCaseCount);
        charGroupsUsed.put("num", minNumCount);
        charGroupsUsed.put("special", minSpecialCount);

        // Because we cannot use the default randomizer, which is based on the
        // current time (it will produce the same "random" number within a
        // second), we will use a random number generator to seed the
        // randomizer.

        // Use a 4-byte array to fill it with random bytes and convert it then
        // to an integer value.
        byte[] randomBytes = new byte[4];

        // Generate 4 random bytes.
        SecureRandom rng = null;
        try {
            rng = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Glue.class.getName()).log(Level.SEVERE, null, ex);
        }
        rng.nextBytes(randomBytes);

        // Convert 4 bytes into a 32-bit integer value.
        int seed = (randomBytes[0] & 0x7f) << 24 |
                randomBytes[1] << 16 |
                randomBytes[2] << 8 |
                randomBytes[3];

        // Create a randomizer from the seed.
        Random random = new Random(seed);

        // Allocate appropriate memory for the password.
        if (minLength < maxLength) {
            randomString = new char[random.nextInt(maxLength - minLength) + minLength];
        } else {
            randomString = new char[minLength];
        }

        int requiredCharactersLeft = minLCaseCount + minUCaseCount + minNumCount + minSpecialCount;

        // Build the password.
        for (int i = 0; i < randomString.length; i++) {
            String selectableChars = "";

            // if we still have plenty of characters left to achieve our minimum requirements.
            if (requiredCharactersLeft < randomString.length - i) {
                // choose from any group at random
                selectableChars = LCaseChars + UCaseChars + NumericChars + SpecialChars;
            } else // we are out of wiggle room, choose from a random group that still needs to have a minimum required.
            {
                // choose only from a group that we need to satisfy a minimum for.
                for (Iterator<String> charGroup = charGroupsUsed.keySet().iterator(); charGroup.hasNext(); ) {
                    String cg = charGroup.next();
                    if (Integer.parseInt(charGroupsUsed.get(cg).toString()) > 0) {
                        if (cg.equalsIgnoreCase("lcase")) {
                            selectableChars += LCaseChars;
                        } else if (cg.equalsIgnoreCase("ucase")) {
                            selectableChars += UCaseChars;
                        } else if (cg.equalsIgnoreCase("num")) {
                            selectableChars += NumericChars;
                        } else if (cg.equalsIgnoreCase("special")) {
                            selectableChars += SpecialChars;
                        }
                    }
                }
            }

            // Now that the string is built, get the next random character.
            String nextChar = new String();
            nextChar += selectableChars.charAt(random.nextInt(selectableChars.length() - 1));

            // Tac it onto our password.
            randomString[i] = nextChar.charAt(0);

            // Now figure out where it came from, and decrement the appropriate minimum value.
            if (LCaseChars.contains(nextChar)) {
                int count = Integer.parseInt(charGroupsUsed.get("lcase").toString()) - 1;
                charGroupsUsed.remove("lcase");
                charGroupsUsed.put("lcase", count);
                if (count >= 0) {
                    requiredCharactersLeft--;
                }
            } else if (UCaseChars.contains(nextChar)) {
                int count = Integer.parseInt(charGroupsUsed.get("ucase").toString()) - 1;
                charGroupsUsed.remove("ucase");
                charGroupsUsed.put("ucase", count);
                if (count >= 0) {
                    requiredCharactersLeft--;
                }
            } else if (NumericChars.contains(nextChar)) {
                int count = Integer.parseInt(charGroupsUsed.get("num").toString()) - 1;
                charGroupsUsed.remove("num");
                charGroupsUsed.put("num", count);
                if (count >= 0) {
                    requiredCharactersLeft--;
                }
            } else if (SpecialChars.contains(nextChar)) {
                int count = Integer.parseInt(charGroupsUsed.get("special").toString()) - 1;
                charGroupsUsed.remove("special");
                charGroupsUsed.put("special", count);
                if (count >= 0) {
                    requiredCharactersLeft--;
                }
            }
        }
        return new String(randomString).getBytes();
    }

    public static synchronized String toString(byte[] input) {
        String result = "";
        for (int i = 0; i < input.length; i++)
            result += (char) input[i];
        return result;
    }

    public SecretKey getKey(char[] password, int pw_strength) {
        // Key Generation
        byte[] salt = {(byte) -12, (byte) 34, (byte) 1, (byte) 0, (byte) -98, (byte) 223, (byte) 78, (byte) 21};
        SecretKeyFactory factory = null;
        SecretKey secret = null;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password, salt, 1024, pw_strength); // Password Strength - n bits
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(Glue.class.getName()).log(Level.SEVERE, null, ex);
        }
        return secret;
    }


    public static synchronized Glue getInstance() {
        if (instance == null) {
            instance = new Glue();
        }
        return instance;
    }
}
