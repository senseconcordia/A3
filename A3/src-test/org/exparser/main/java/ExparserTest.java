package org.exparser.main.java;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class ExparserTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void incorrectArgumentNumber(){

        String[] tooFewArguments = {};
        try {
            Exparser.main(tooFewArguments);
        }
        catch (Exception e){
            exception.expect(IllegalArgumentException.class);
            exception.expectMessage("Type 'Exparser -h' for help.");
            throw new IllegalArgumentException(e);
        }

    }
}