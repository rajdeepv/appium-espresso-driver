package io.appium.espressoserver.lib.helpers;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import io.appium.espressoserver.lib.handlers.exceptions.InvalidStrategyException;
import io.appium.espressoserver.lib.handlers.exceptions.XPathLookupException;
import io.appium.espressoserver.lib.model.Strategy;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static io.appium.espressoserver.lib.viewmatcher.WithXPath.withXPath;
import static org.hamcrest.Matchers.endsWith;

/**
 * Helper methods to find elements based on locator strategies and selectors
 */
public class ViewFinder {

    /**
     * Find one instance of an element that matches the locator criteria
     * @param strategy Locator strategy (xpath, class name, etc...)
     * @param selector Selector string
     * @return
     * @throws InvalidStrategyException
     * @throws XPathLookupException
     */
    @Nullable
    public static ViewOrDataInteraction findBy(Strategy strategy, String selector) throws InvalidStrategyException, XPathLookupException {
        List<ViewOrDataInteraction> viewInteractions = findAllBy(strategy, selector, true);
        if (viewInteractions.isEmpty()) {
            return null;
        }
        return viewInteractions.get(0);
    }

    /**
     * Find all instances of an element that matches the locator criteria
     * @param strategy Locator strategy (xpath, class name, etc...)
     * @param selector Selector string
     * @return
     * @throws InvalidStrategyException
     * @throws XPathLookupException
     */
    public static List<ViewOrDataInteraction> findAllBy(Strategy strategy, String selector) throws InvalidStrategyException, XPathLookupException {
        return findAllBy(strategy, selector, false);
    }

    ///Find By different strategies
    private static List<ViewOrDataInteraction> findAllBy(Strategy strategy, String selector, boolean findOne)
            throws InvalidStrategyException, XPathLookupException {
        List<ViewOrDataInteraction> matcher;

        switch (strategy) {
            case ID: // with ID

                // find id from target context
                Context context = InstrumentationRegistry.getTargetContext();
                int id = context.getResources().getIdentifier(selector, "Id",
                        InstrumentationRegistry.getTargetContext().getPackageName());

                matcher = getViewOrDataInteractions(withId(id), findOne);
                break;
            case CLASS_NAME:
                // with class name
                // TODO: improve this finder with instanceOf
                matcher = getViewOrDataInteractions(withClassName(endsWith(selector)), findOne);
                break;
            case TEXT:
                // with text
                matcher = getViewOrDataInteractions(withText(selector), findOne);
                break;
            case ACCESSIBILITY_ID:
                // with content description
                matcher = getViewOrDataInteractions(withContentDescription(selector), findOne);
                break;
            case XPATH:
                // If we're only looking for one item that matches xpath, pass it index 0 or else
                // Espresso throws an AmbiguousMatcherException
                if (findOne) {
                    matcher = getViewOrDataInteractions(withXPath(selector, 0), true);
                } else {
                    matcher = getViewOrDataInteractions(withXPath(selector), false);
                }
                break;
            /*case ESPRESSO_HAMCREST:
                System.out.println("Calling Espresso hamcrest matcher");
                break;*/
            default:
                throw new InvalidStrategyException(String.format("Strategy is not implemented: %s", strategy.getStrategyName()));
        }

        return matcher;
    }

    private static List<ViewOrDataInteraction> getViewOrDataInteractions(Matcher<View> matcher, boolean findOne) {
        // If it's just one view we want, return a singleton list
        if (findOne) {
            return Collections.singletonList(new ViewOrDataInteraction(onView(matcher)));
        }

        // If we want all views that match the criteria, start looking for ViewInteractions by
        // index and add each match to the List. As soon as we find no match, break the loop
        // and return the list
        List<ViewOrDataInteraction> interactions = new ArrayList<>();
        int i = 0;
        do {
            ViewOrDataInteraction viewInteraction = new ViewOrDataInteraction(onView(withIndex(matcher, i)));
            try {
                viewInteraction.check(matches(isDisplayed()));
            } catch (NoMatchingViewException nme) {
                break;
            }
            interactions.add(viewInteraction);
            i++;
        } while (i < Integer.MAX_VALUE);
        return interactions;
    }

    private static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                return matcher.matches(view) && currentIndex++ == index;
            }
        };
    }
}