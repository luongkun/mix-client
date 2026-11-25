package mix.client.mixui.visuals.furiah.module.trails;

public final class TrailPreviews {
    public enum View {
        SIDE, BEHIND, ORBIT
    }

    public boolean day;
    public boolean showOther;
    public View view = View.SIDE;
    private final TrailPreview big = new TrailPreview();

    public TrailPreviews(TrailsModule module) {
    }

    public TrailPreview big() {
        return big;
    }

    public float drawMillisForTest() {
        return 0.0F;
    }

    public TrailPreview cardForTest(TrailStyle type) {
        return null;
    }
}
