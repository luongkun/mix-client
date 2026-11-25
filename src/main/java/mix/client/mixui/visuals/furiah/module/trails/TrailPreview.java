package mix.client.mixui.visuals.furiah.module.trails;

public final class TrailPreview {
    public enum Move {
        WALK, SPRINT, JUMP, SNEAK, STOP
    }

    public Move move = Move.WALK;
    public final PlayerTrail trail = new PlayerTrail(0, true);
}
