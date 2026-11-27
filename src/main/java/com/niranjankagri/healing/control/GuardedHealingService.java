package com.niranjankagri.healing.control;

import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.model.Context;
import com.epam.healenium.model.HealedElement;
import com.epam.healenium.model.HealingResult;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.SelectorImitatorDto;
import com.epam.healenium.service.HealingService;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Healenium's own healing, plus the two checks of the {@link HealingPolicy}. Installed with
 * {@code engine.setHealingService(...)}: Healenium asks the engine for its healing service on every lookup.
 *
 * <p>Healenium ranks every element on the page against the saved reference and keeps the ones above
 * {@code score-cap}, best first ({@link HealingResult#getTargetNodes()}). It then uses the best one, however close
 * the runner-up is. This class looks at that same ranked list after Healenium has picked, records the heal with
 * its evidence, and (in enforce mode) stops it when the policy says no. Stopping means: forget the proposed heal
 * and throw {@link HealingStoppedException}, so the test sees "element not found" with the reason, and Healenium
 * neither uses nor saves the heal.
 */
public class GuardedHealingService extends HealingService {

    /** Page objects live here; the first such frame in the stack is the caller a reviewer cares about. */
    private static final String PAGES_PACKAGE = "com.niranjankagri.healing.pages";

    private final HealingPolicy policy;
    private final HealGuard guard;
    private final HealingJournal journal;

    public GuardedHealingService(SelfHealingEngine engine, HealingPolicy policy, ReviewedHeals reviewed,
                                 HealingJournal journal) {
        super(engine.getConfig(), engine.getWebDriver());
        this.policy = policy;
        this.guard = new HealGuard(policy, reviewed);
        this.journal = journal;
    }

    @Override
    public void findNewLocations(List<Node> paths, Node destination, Context context, SelfHealingEngine engine) {
        int before = context.getHealingResults().size();
        super.findNewLocations(paths, destination, context, engine);
        if (context.getHealingResults().size() == before) {
            return; // nothing scored above score-cap: Healenium throws "not found" itself
        }
        HealingResult proposed = context.getHealingResults().get(context.getHealingResults().size() - 1);
        List<Scored<Node>> ranked = proposed.getTargetNodes();
        double score = ranked.get(0).getScore();
        Double runnerUp = ranked.size() > 1 ? ranked.get(1).getScore() : null;

        HealKey key = new HealKey(caller(), context.getBy().toString(), finalLocator(proposed, context, engine));
        HealGuard.Verdict verdict = guard.decide(key, score, runnerUp);
        boolean stop = policy.enforce() && verdict.decision() != HealDecision.USED;

        HealDecision recorded = stop ? verdict.decision() : HealDecision.USED;
        String reason = !stop && verdict.decision() != HealDecision.USED
                ? "Observe mode, used anyway. Would stop: " + verdict.reason()
                : verdict.reason();
        journal.add(new HealRecord(journal.currentTest(), key.pageObject(), key.locator(), key.healedTo(), score,
                runnerUp, recorded, reason, ranked.stream().limit(HealRecord.MAX_CANDIDATES).map(Candidate::of).toList(),
                journal.saveScreenshot(screenshot(proposed.getHealedElements().get(0).getElement()))));

        if (stop) {
            context.getHealingResults().remove(proposed);
            throw new HealingStoppedException(verdict.decision(),
                    "Healing stopped for " + key.locator() + " in " + key.pageObject() + ": " + verdict.reason());
        }
    }

    /**
     * The locator Healenium will report for this heal. Healenium builds it later (ImitateProcessor) by asking the
     * selector-imitator for a locator of the same kind as the broken one; the same two calls here give the same
     * result now, so a rejected heal can be recognised before it is used.
     */
    private static String finalLocator(HealingResult proposed, Context context, SelfHealingEngine engine) {
        HealedElement chosen = proposed.getHealedElements().get(0);
        Scored<Node> best = proposed.getTargetNodes().get(0);
        List<Locator> imitated = engine.getClient().imitate(new SelectorImitatorDto(best.getValue(), context.getUserLocator()));
        engine.replaceHealedElementLocator(imitated, best.getScore(), chosen);
        return chosen.getScored().getValue().toString();
    }

    /** {@code ProductCard#buttonLabel}: the page-object method whose lookup was healed. */
    private static String caller() {
        return StackWalker.getInstance().walk(frames -> frames
                .filter(f -> f.getClassName().startsWith(PAGES_PACKAGE))
                .findFirst()
                .map(f -> f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1) + "#" + f.getMethodName())
                .orElse("(outside page objects)"));
    }

    /**
     * The page with the proposed element outlined in red, so a reviewer sees at a glance which element Healenium
     * picked. The element is restored straight after, attribute for attribute: Healenium compares attributes when
     * it scores later lookups, so even an empty {@code style=""} left behind could change a score.
     */
    private byte[] screenshot(WebElement proposed) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object before = null;
        try {
            before = js.executeScript(
                    "const e = arguments[0]; const s = e.getAttribute('style');"
                            + " e.style.outline = '3px solid red'; return s;", proposed);
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (RuntimeException e) {
            return null; // evidence is best effort; the decision stands without it
        } finally {
            try {
                js.executeScript(
                        "const e = arguments[0]; const s = arguments[1];"
                                + " if (s === null) { e.removeAttribute('style'); } else { e.setAttribute('style', s); }",
                        proposed, before);
            } catch (RuntimeException ignored) {
                // the page may have changed; nothing to restore
            }
        }
    }
}
