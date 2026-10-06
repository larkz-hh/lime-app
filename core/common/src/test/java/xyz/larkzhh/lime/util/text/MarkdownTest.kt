package xyz.larkzhh.lime.util.text

import org.junit.Assert.assertEquals
import org.junit.Test

/// Markdown 符号剥离
class MarkdownTest {

    @Test
    fun 空串返回空串() {
        assertEquals("", "".stripMarkdown())
    }

    @Test
    fun 标题符号被剥离() {
        assertEquals("标题", "# 标题".stripMarkdown())
        assertEquals("三级标题", "### 三级标题".stripMarkdown())
    }

    @Test
    fun 行内代码去掉反引号保留内容() {
        assertEquals("用 println 输出", "用 `println` 输出".stripMarkdown())
    }

    @Test
    fun 代码块整体被丢弃() {
        // 整段替换成空格后再折叠，所以前后之间留一个空格
        assertEquals("前 后", "前\n```kotlin\nval x = 1\n```\n后".stripMarkdown())
    }

    @Test
    fun 链接只留文字() {
        assertEquals("点这里", "[点这里](https://a.com)".stripMarkdown())
    }

    @Test
    fun 图片只留替代文字() {
        assertEquals("图片说明", "![图片说明](https://a.com/x.png)".stripMarkdown())
    }

    @Test
    fun 无序列表符号被剥离() {
        assertEquals("第一项 第二项", "- 第一项\n- 第二项".stripMarkdown())
        assertEquals("星号项", "* 星号项".stripMarkdown())
    }

    @Test
    fun 有序列表符号被剥离() {
        assertEquals("一 二", "1. 一\n2. 二".stripMarkdown())
    }

    @Test
    fun 引用符号被剥离() {
        assertEquals("引用内容", "> 引用内容".stripMarkdown())
    }

    @Test
    fun 加粗与斜体标记被删除() {
        assertEquals("重点", "**重点**".stripMarkdown())
        assertEquals("斜体", "_斜体_".stripMarkdown())
        assertEquals("又粗又斜", "***又粗又斜***".stripMarkdown())
    }

    @Test
    fun 表格竖线变空格() {
        assertEquals("a b", "| a | b |".stripMarkdown())
    }

    @Test
    fun 多行用空格连接() {
        assertEquals("第一行 第二行", "第一行\n第二行".stripMarkdown())
    }

    @Test
    fun 连续空白折叠成一个() {
        assertEquals("a b", "a    b".stripMarkdown())
        assertEquals("a b", "  a  \n\n  b  ".stripMarkdown())
    }
}
