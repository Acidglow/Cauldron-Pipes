package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
    private int attempts = 0;
    private int successes = 0;

    public ReportGameListener() {
    }

    @Override
    public void testStructureLoaded(GameTestInfo p_177718_) {
        this.attempts++;
    }

    private void handleRetry(GameTestInfo testInfo, GameTestRunner runner, boolean passed) {
        RetryOptions retryoptions = testInfo.retryOptions();
        String s = String.format(Locale.ROOT, "[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
        if (!retryoptions.unlimitedTries()) {
            s = s + String.format(Locale.ROOT, ", Left: %4d", retryoptions.numberOfTries() - this.attempts);
        }

        s = s + "]";
        String s1 = testInfo.id() + " " + (passed ? "passed" : "failed") + "! " + testInfo.getRunTime() + "ms";
        String s2 = String.format(Locale.ROOT, "%-53s%s", s, s1);
        if (passed) {
            reportPassed(testInfo, s2);
        } else {
            say(testInfo.getLevel(), ChatFormatting.RED, s2);
        }

        if (retryoptions.hasTriesLeft(this.attempts, this.successes)) {
            runner.rerunTest(testInfo);
        }
    }

    @Override
    public void testPassed(GameTestInfo p_177729_, GameTestRunner p_320359_) {
        this.successes++;
        if (p_177729_.retryOptions().hasRetries()) {
            this.handleRetry(p_177729_, p_320359_, true);
        } else if (!p_177729_.isFlaky()) {
            reportPassed(p_177729_, p_177729_.id() + " passed! (" + p_177729_.getRunTime() + "ms / " + p_177729_.getTick() + "gameticks)");
        } else {
            if (this.successes >= p_177729_.requiredSuccesses()) {
                reportPassed(p_177729_, p_177729_ + " passed " + this.successes + " times of " + this.attempts + " attempts.");
            } else {
                say(
                    p_177729_.getLevel(),
                    ChatFormatting.GREEN,
                    "Flaky test " + p_177729_ + " succeeded, attempt: " + this.attempts + " successes: " + this.successes
                );
                p_320359_.rerunTest(p_177729_);
            }
        }
    }

    @Override
    public void testFailed(GameTestInfo p_177737_, GameTestRunner p_320181_) {
        if (!p_177737_.isFlaky()) {
            reportFailure(p_177737_, p_177737_.getError());
            if (p_177737_.retryOptions().hasRetries()) {
                this.handleRetry(p_177737_, p_320181_, false);
            }
        } else {
            GameTestInstance gametestinstance = p_177737_.getTest();
            String s = "Flaky test " + p_177737_ + " failed, attempt: " + this.attempts + "/" + gametestinstance.maxAttempts();
            if (gametestinstance.requiredSuccesses() > 1) {
                s = s + ", successes: " + this.successes + " (" + gametestinstance.requiredSuccesses() + " required)";
            }

            say(p_177737_.getLevel(), ChatFormatting.YELLOW, s);
            if (p_177737_.maxAttempts() - this.attempts + this.successes >= p_177737_.requiredSuccesses()) {
                p_320181_.rerunTest(p_177737_);
            } else {
                reportFailure(p_177737_, new ExhaustedAttemptsException(this.attempts, this.successes, p_177737_));
            }
        }
    }

    @Override
    public void testAddedForRerun(GameTestInfo p_320478_, GameTestInfo p_320907_, GameTestRunner p_320607_) {
        p_320907_.addListener(this);
    }

    public static void reportPassed(GameTestInfo testInfo, String message) {
        getTestInstanceBlockEntity(testInfo).ifPresent(p_396406_ -> p_396406_.setSuccess());
        visualizePassedTest(testInfo, message);
    }

    private static void visualizePassedTest(GameTestInfo testInfo, String message) {
        say(testInfo.getLevel(), ChatFormatting.GREEN, message);
        GlobalTestReporter.onTestSuccess(testInfo);
    }

    protected static void reportFailure(GameTestInfo testInfo, Throwable error) {
        Component component;
        if (error instanceof GameTestAssertException gametestassertexception) {
            component = gametestassertexception.getDescription();
        } else {
            component = Component.literal(Util.describeError(error));
        }

        getTestInstanceBlockEntity(testInfo).ifPresent(p_396408_ -> p_396408_.setErrorMessage(component));
        visualizeFailedTest(testInfo, error);
    }

    protected static void visualizeFailedTest(GameTestInfo testInfo, Throwable error) {
        String s = error.getMessage() + (error.getCause() == null ? "" : " cause: " + Util.describeError(error.getCause()));
        String s1 = (testInfo.isRequired() ? "" : "(optional) ") + testInfo.id() + " failed! " + s;
        say(testInfo.getLevel(), testInfo.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, s1);
        Throwable throwable = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(error), error);
        if (throwable instanceof GameTestAssertPosException gametestassertposexception) {
            testInfo.getTestInstanceBlockEntity().markError(gametestassertposexception.getAbsolutePos(), gametestassertposexception.getMessageToShowAtBlock());
        }

        GlobalTestReporter.onTestFailed(testInfo);
    }

    private static Optional<TestInstanceBlockEntity> getTestInstanceBlockEntity(GameTestInfo testInfo) {
        ServerLevel serverlevel = testInfo.getLevel();
        Optional<BlockPos> optional = Optional.ofNullable(testInfo.getTestBlockPos());
        return optional.flatMap(p_396405_ -> serverlevel.getBlockEntity(p_396405_, BlockEntityType.TEST_INSTANCE_BLOCK));
    }

    protected static void say(ServerLevel serverLevel, ChatFormatting formatting, String message) {
        serverLevel.getPlayers(p_177705_ -> true).forEach(p_177709_ -> p_177709_.sendSystemMessage(Component.literal(message).withStyle(formatting)));
    }
}
