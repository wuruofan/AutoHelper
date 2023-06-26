package net.taikula.autohelper.algorithm

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import java.io.IOException


/**
 * 图片相似度比较 pHash 算法
 *
 * pHash算法流程
 * 1.缩小图片，最佳大小为32*32
 * 2.转化成灰度图
 * 3.转化为DCT图
 * 4.取dct图左上角8*8的范围
 * 5.计算所有点的平均值
 * 6.8*8的范围刚好64个点，计算出64位的图片指纹，如果小于平均值记为0，反之记为1，指纹顺序可以随机，但是每张图片的指纹的顺序应该保持一致
 * 7.最后比较两张图片指纹的汉明距离，越小表示越相识
 *
 */
object pHash {
    /**
     * 获取图片 Hash 指纹，long刚好64位，方便存放
     */
    @Throws(IOException::class)
    fun dctImageHash(src: Bitmap?, recycle: Boolean = false): Long {
        //由于计算dct需要图片长宽相等，所以统一取32
        val length = 32

        //缩放图片
        val bitmap = scaleBitmap(src, recycle, length.toFloat())

        //获取灰度图
        val pixels = createGrayImage(bitmap, length)

        //先获得32*32的dct，再取dct左上角8*8的区域
        return computeHash(DCT8(pixels, length))
    }

    /**
     * 创建灰度图片
     */
    private fun createGrayImage(src: Bitmap, length: Int): IntArray {
        val pixels = IntArray(length * length)
        src.getPixels(pixels, 0, length, 0, 0, length, length)
        src.recycle()
        for (i in pixels.indices) {
            val gray = computeGray(pixels[i])
            pixels[i] = Color.rgb(gray, gray, gray)
        }
        return pixels
    }

    /**
     * 缩放成宽高一样的图片
     */
    @Throws(IOException::class)
    private fun scaleBitmap(src: Bitmap?, recycle: Boolean, length: Float): Bitmap {
        if (src == null) {
            throw IOException("invalid image")
        }

        val width = src.width
        val height = src.height
        if (width == 0 || height == 0) {
            throw IOException("invalid image")
        }
        val matrix = Matrix()
        matrix.postScale(length / width, length / height)
        val bitmap = Bitmap.createBitmap(src, 0, 0, width, height, matrix, false)
        if (recycle) {
            src.recycle()
        }
        return bitmap
    }

    /**
     * 计算hash值
     */
    private fun computeHash(pxs: DoubleArray): Long {
        var t = 0.0
        for (i in pxs) {
            t += i
        }
        val median = t / pxs.size
        var one: Long = 0x0000000000000001
        var hash: Long = 0x0000000000000000
        for (current in pxs) {
            if (current > median) hash = hash or one
            one = one shl 1
        }
        return hash
    }

    /**
     * 计算灰度值
     * 计算公式Gray = R*0.299 + G*0.587 + B*0.114
     * 由于浮点数运算性能较低，转换成位移运算
     * 向右每位移一位，相当于除以2
     */
    private fun computeGray(pixel: Int): Int {
        val red: Int = Color.red(pixel)
        val green: Int = Color.green(pixel)
        val blue: Int = Color.blue(pixel)
        return red * 38 + green * 75 + blue * 15 shr 7
    }

    /**
     * 取dct图左上角8*8的区域
     */
    private fun DCT8(pix: IntArray, n: Int): DoubleArray {
        val iMatrix = DCT(pix, n)
        val px = DoubleArray(8 * 8)
        for (i in 0..7) {
            System.arraycopy(iMatrix[i], 0, px, i * 8, 8)
        }
        return px
    }

    /**
     * 离散余弦变换
     *
     * 计算公式为：系数矩阵*图片矩阵*转置系数矩阵
     *
     * @param pix 原图像的数据矩阵
     * @param n   原图像(n*n)
     * @return 变换后的矩阵数组
     */
    private fun DCT(pix: IntArray, n: Int): Array<DoubleArray> {
        var iMatrix = Array(n) {
            DoubleArray(
                n
            )
        }
        for (i in 0 until n) {
            for (j in 0 until n) {
                iMatrix[i][j] = pix[i * n + j].toDouble()
            }
        }
        val quotient = coefficient(n) //求系数矩阵
        val quotientT = transposingMatrix(quotient, n) //转置系数矩阵
        val temp: Array<DoubleArray> = matrixMultiply(quotient, iMatrix, n)
        iMatrix = matrixMultiply(temp, quotientT, n)
        return iMatrix
    }

    /**
     * 矩阵转置
     *
     * @param matrix 原矩阵
     * @param n      矩阵(n*n)
     * @return 转置后的矩阵
     */
    private fun transposingMatrix(matrix: Array<DoubleArray>, n: Int): Array<DoubleArray> {
        val nMatrix = Array(n) {
            DoubleArray(
                n
            )
        }
        for (i in 0 until n) {
            for (j in 0 until n) {
                nMatrix[i][j] = matrix[j][i]
            }
        }
        return nMatrix
    }

    /**
     * 求离散余弦变换的系数矩阵
     *
     * @param n n*n矩阵的大小
     * @return 系数矩阵
     */
    private fun coefficient(n: Int): Array<DoubleArray> {
        val coeff = Array(n) {
            DoubleArray(
                n
            )
        }
        val sqrt = Math.sqrt(1.0 / n)
        val sqrt1 = Math.sqrt(2.0 / n)
        for (i in 0 until n) {
            coeff[0][i] = sqrt
        }
        for (i in 1 until n) {
            for (j in 0 until n) {
                coeff[i][j] = sqrt1 * Math.cos(i * Math.PI * (j + 0.5) / n)
            }
        }
        return coeff
    }

    /**
     * 矩阵相乘
     *
     * @param A 矩阵A
     * @param B 矩阵B
     * @param n 矩阵的大小n*n
     * @return 结果矩阵
     */
    private fun matrixMultiply(
        A: Array<DoubleArray>,
        B: Array<DoubleArray>,
        n: Int
    ): Array<DoubleArray> {
        val nMatrix = Array(n) {
            DoubleArray(
                n
            )
        }
        var t: Double
        for (i in 0 until n) {
            for (j in 0 until n) {
                t = 0.0
                for (k in 0 until n) {
                    t += A[i][k] * B[k][j]
                }
                nMatrix[i][j] = t
            }
        }
        return nMatrix
    }

    /**
     * 计算两个图片指纹的汉明距离
     *
     * @param hash1 指纹1
     * @param hash2 指纹2
     * @return 返回汉明距离 也就是64位long型不相同的位的个数
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        var x = hash1 xor hash2
        val m1 = 0x5555555555555555L
        val m2 = 0x3333333333333333L
        val h01 = 0x0101010101010101L
        val m4 = 0x0f0f0f0f0f0f0f0fL
        x -= x shr 1 and m1
        x = (x and m2) + (x shr 2 and m2)
        x = x + (x shr 4) and m4
        return (x * h01 shr 56).toInt()
    }


    /**
     * 暂定相同点小于10%为相似
     */
    fun isSimilar(distance: Int): Boolean {
        return distance >= 0 && distance < (64 * 0.1f).toInt()
    }
}