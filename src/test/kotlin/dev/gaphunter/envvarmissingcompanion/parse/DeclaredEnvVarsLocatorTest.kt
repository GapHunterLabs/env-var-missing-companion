package dev.gaphunter.envvarmissingcompanion.parse

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeclaredEnvVarsLocatorTest {

    @Test
    fun `recognizes plain dot-env`() {
        assertTrue(DeclaredEnvVarsLocator.isEnvFile(".env"))
    }

    @Test
    fun `recognizes dot-env-example`() {
        assertTrue(DeclaredEnvVarsLocator.isEnvFile(".env.example"))
    }

    @Test
    fun `recognizes dot-env-local and dot-env-production`() {
        assertTrue(DeclaredEnvVarsLocator.isEnvFile(".env.local"))
        assertTrue(DeclaredEnvVarsLocator.isEnvFile(".env.production"))
    }

    @Test
    fun `rejects unrelated dotfiles`() {
        assertFalse(DeclaredEnvVarsLocator.isEnvFile(".environment"))
        assertFalse(DeclaredEnvVarsLocator.isEnvFile(".eslintrc"))
        assertFalse(DeclaredEnvVarsLocator.isEnvFile("env.js"))
    }

    @Test
    fun `rejects regular source files`() {
        assertFalse(DeclaredEnvVarsLocator.isEnvFile("index.js"))
        assertFalse(DeclaredEnvVarsLocator.isEnvFile("settings.py"))
    }
}
