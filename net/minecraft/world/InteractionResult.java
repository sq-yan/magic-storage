package net.minecraft.world;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public sealed interface InteractionResult
    permits InteractionResult.Success,
    InteractionResult.Fail,
    InteractionResult.Pass,
    InteractionResult.TryEmptyHandInteraction {
    InteractionResult.Success f_19068_ = new InteractionResult.Success(InteractionResult.SwingSource.CLIENT, InteractionResult.ItemContext.f_349413_);
    InteractionResult.Success f_349050_ = new InteractionResult.Success(InteractionResult.SwingSource.SERVER, InteractionResult.ItemContext.f_349413_);
    InteractionResult.Success f_19069_ = new InteractionResult.Success(InteractionResult.SwingSource.NONE, InteractionResult.ItemContext.f_349413_);
    InteractionResult.Fail f_19071_ = new InteractionResult.Fail();
    InteractionResult.Pass f_19070_ = new InteractionResult.Pass();
    InteractionResult.TryEmptyHandInteraction f_348895_ = new InteractionResult.TryEmptyHandInteraction();

    default boolean m_19077_() {
        return false;
    }

    public record Fail() implements InteractionResult {
    }

    public record ItemContext(boolean f_346918_, @Nullable ItemStack f_349253_) {
        static InteractionResult.ItemContext f_348861_ = new InteractionResult.ItemContext(false, null);
        static InteractionResult.ItemContext f_349413_ = new InteractionResult.ItemContext(true, null);
    }

    public record Pass() implements InteractionResult {
    }

    public record Success(InteractionResult.SwingSource f_347780_, InteractionResult.ItemContext f_347503_) implements InteractionResult {
        @Override
        public boolean m_19077_() {
            return true;
        }

        public InteractionResult.Success m_357016_(ItemStack p_362659_) {
            return new InteractionResult.Success(this.f_347780_, new InteractionResult.ItemContext(true, p_362659_));
        }

        public InteractionResult.Success m_353129_() {
            return new InteractionResult.Success(this.f_347780_, InteractionResult.ItemContext.f_348861_);
        }

        public boolean m_351618_() {
            return this.f_347503_.f_346918_;
        }

        public @Nullable ItemStack m_354149_() {
            return this.f_347503_.f_349253_;
        }
    }

    public static enum SwingSource {
        NONE,
        CLIENT,
        SERVER;
    }

    public record TryEmptyHandInteraction() implements InteractionResult {
    }
}
