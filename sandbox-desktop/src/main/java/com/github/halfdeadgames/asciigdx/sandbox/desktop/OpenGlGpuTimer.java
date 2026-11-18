package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import com.github.halfdeadgames.asciigdx.sandbox.GpuTimer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/** GPU timer built on OpenGL 3.3 timer queries (GL_TIME_ELAPSED). */
final class OpenGlGpuTimer implements GpuTimer {
    private static final int NO_QUERY = 0;

    private int query = NO_QUERY;
    private Boolean available;

    @Override
    public boolean isAvailable() {
        if (available == null) {
            available = GL.getCapabilities().OpenGL33;
        }
        return available;
    }

    @Override
    public void begin() {
        if (query == NO_QUERY) {
            query = GL15.glGenQueries();
        }
        GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query);
    }

    @Override
    public void end() {
        GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
    }

    @Override
    public long readElapsedNanos() {
        return GL33.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT);
    }
}
