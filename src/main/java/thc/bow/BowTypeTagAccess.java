package thc.bow;

/**
 * Duck interface for accessing bow type tag from ProjectileEntityMixin.
 * Implemented by ProjectileEntityMixin, used by AbstractArrowMixin
 * to read the bow type tag for damage multiplier logic.
 */
public interface BowTypeTagAccess {
	String thc$getBowTypeTag();
}
