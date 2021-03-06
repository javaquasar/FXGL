/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxgl.core.math

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Test

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class Vec2Test {

    @Test
    fun `Test equality`() {
        val v1 = Vec2()
        val v2 = Vec2()

        assertThat(v1, `is`(v1))
        assertThat(v1, `is`(v2))
        assertThat(v2, `is`(v1))
        assertThat(v1.hashCode(), `is`(v2.hashCode()))

        v2.x = 10.0f
        assertThat(v1, `is`(not(v2)))
        assertThat(v1.hashCode(), `is`(not(v2.hashCode())))

        v1.x = 10.0f
        assertThat(v1, `is`(v2))
        assertThat(v1.hashCode(), `is`(v2.hashCode()))

        v2.y = -3.0f
        assertThat(v1, `is`(not(v2)))
        assertThat(v1.hashCode(), `is`(not(v2.hashCode())))

        v1.y = -3.0f
        assertThat(v1, `is`(v2))
        assertThat(v1.hashCode(), `is`(v2.hashCode()))
    }
}