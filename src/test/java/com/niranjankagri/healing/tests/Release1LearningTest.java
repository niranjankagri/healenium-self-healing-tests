package com.niranjankagri.healing.tests;

import com.niranjankagri.healing.app.Release;
import com.niranjankagri.healing.driver.DriverFactory;

/**
 * Step 1: the journeys on release 1, where every locator matches. Nothing is healed; Healenium saves the element
 * each locator found (tag, attributes, text, position in the page) as the reference for later runs.
 */
public class Release1LearningTest extends ShopJourneys {

    @Override
    protected Release release() {
        return Release.V1;
    }

    @Override
    protected DriverFactory.Kind driverKind() {
        return DriverFactory.Kind.SELF_HEALING;
    }
}
