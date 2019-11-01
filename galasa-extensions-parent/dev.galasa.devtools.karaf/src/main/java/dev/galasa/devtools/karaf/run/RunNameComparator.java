/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf.run;

import java.util.Comparator;

import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.ResultArchiveStoreException;

public class RunNameComparator implements Comparator<IRunResult> {

    @Override
    public int compare(IRunResult arg0, IRunResult arg1) {
        try {
            return arg0.getTestStructure().getRunName().compareTo(arg1.getTestStructure().getRunName());
        } catch (ResultArchiveStoreException e) {
            return 0;
        }
    }

}
