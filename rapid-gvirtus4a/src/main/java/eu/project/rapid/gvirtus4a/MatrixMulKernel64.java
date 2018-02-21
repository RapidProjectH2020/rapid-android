package eu.project.rapid.gvirtus4a;

/**
 * Created by raffaelemontella on 20/02/2018.
 */

public class MatrixMulKernel64 {
    public static String getPtxSource() { return ptxSource; }
    public static final String ptxSource="\t.version 1.4\n" +
            "\t.target sm_11, map_f64_to_f32\n" +
            "\t// compiled with /usr/local/cuda-6.5/open64/lib//be\n" +
            "\t// nvopencc 4.1 built on 2014-06-19\n" +
            "\n" +
            "\t//-----------------------------------------------------------\n" +
            "\t// Compiling /tmp/tmpxft_000024c7_00000000-9_matrixMul_kernel.cpp3.i (/tmp/ccBI#.ypLfpy)\n" +
            "\t//-----------------------------------------------------------\n" +
            "\n" +
            "\t//-----------------------------------------------------------\n" +
            "\t// Options:\n" +
            "\t//-----------------------------------------------------------\n" +
            "\t//  Target:ptx, ISA:sm_11, Endian:little, Pointer Size:64\n" +
            "\t//  -O3\t(Optimization level)\n" +
            "\t//  -g0\t(Debug level)\n" +
            "\t//  -m2\t(Report advisories)\n" +
            "\t//-----------------------------------------------------------\n" +
            "\n" +
            "\t.file\t1\t\"<command-line>\"\n" +
            "\t.file\t2\t\"/usr/include/stdc-predef.h\"\n" +
            "\t.file\t3\t\"/tmp/tmpxft_000024c7_00000000-8_matrixMul_kernel.cudafe2.gpu\"\n" +
            "\t.file\t4\t\"/usr/lib/gcc/x86_64-linux-gnu/4.8/include/stddef.h\"\n" +
            "\t.file\t5\t\"/usr/local/cuda/include/crt/device_runtime.h\"\n" +
            "\t.file\t6\t\"/usr/local/cuda/include/host_defines.h\"\n" +
            "\t.file\t7\t\"/usr/local/cuda/include/builtin_types.h\"\n" +
            "\t.file\t8\t\"/usr/local/cuda/include/device_types.h\"\n" +
            "\t.file\t9\t\"/usr/local/cuda/include/driver_types.h\"\n" +
            "\t.file\t10\t\"/usr/local/cuda/include/surface_types.h\"\n" +
            "\t.file\t11\t\"/usr/local/cuda/include/texture_types.h\"\n" +
            "\t.file\t12\t\"/usr/local/cuda/include/vector_types.h\"\n" +
            "\t.file\t13\t\"/usr/local/cuda/include/device_launch_parameters.h\"\n" +
            "\t.file\t14\t\"/usr/local/cuda/include/crt/storage_class.h\"\n" +
            "\t.file\t15\t\"matrixMul_kernel.cu\"\n" +
            "\t.file\t16\t\"/usr/local/cuda/include/common_functions.h\"\n" +
            "\t.file\t17\t\"/usr/local/cuda/include/math_functions.h\"\n" +
            "\t.file\t18\t\"/usr/local/cuda/include/math_constants.h\"\n" +
            "\t.file\t19\t\"/usr/local/cuda/include/device_functions.h\"\n" +
            "\t.file\t20\t\"/usr/local/cuda/include/sm_11_atomic_functions.h\"\n" +
            "\t.file\t21\t\"/usr/local/cuda/include/sm_12_atomic_functions.h\"\n" +
            "\t.file\t22\t\"/usr/local/cuda/include/sm_13_double_functions.h\"\n" +
            "\t.file\t23\t\"/usr/local/cuda/include/sm_20_atomic_functions.h\"\n" +
            "\t.file\t24\t\"/usr/local/cuda/include/sm_32_atomic_functions.h\"\n" +
            "\t.file\t25\t\"/usr/local/cuda/include/sm_35_atomic_functions.h\"\n" +
            "\t.file\t26\t\"/usr/local/cuda/include/sm_20_intrinsics.h\"\n" +
            "\t.file\t27\t\"/usr/local/cuda/include/sm_30_intrinsics.h\"\n" +
            "\t.file\t28\t\"/usr/local/cuda/include/sm_32_intrinsics.h\"\n" +
            "\t.file\t29\t\"/usr/local/cuda/include/sm_35_intrinsics.h\"\n" +
            "\t.file\t30\t\"/usr/local/cuda/include/surface_functions.h\"\n" +
            "\t.file\t31\t\"/usr/local/cuda/include/texture_fetch_functions.h\"\n" +
            "\t.file\t32\t\"/usr/local/cuda/include/texture_indirect_functions.h\"\n" +
            "\t.file\t33\t\"/usr/local/cuda/include/surface_indirect_functions.h\"\n" +
            "\t.file\t34\t\"/usr/local/cuda/include/math_functions_dbl_ptx1.h\"\n" +
            "\n" +
            "\t.shared .align 4 .b8 __cuda_local_var_16455_39_non_const_As__6[1024];\n" +
            "\t.shared .align 4 .b8 __cuda_local_var_16459_39_non_const_Bs__7[1024];\n" +
            "\n" +
            "\t.entry matrixMul_bs16_32bit (\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs16_32bit_C,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs16_32bit_A,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs16_32bit_B,\n" +
            "\t\t.param .s32 __cudaparm_matrixMul_bs16_32bit_wA,\n" +
            "\t\t.param .s32 __cudaparm_matrixMul_bs16_32bit_wB)\n" +
            "\t{\n" +
            "\t.reg .u32 %r<34>;\n" +
            "\t.reg .u64 %rd<29>;\n" +
            "\t.reg .f32 %f<52>;\n" +
            "\t.reg .pred %p<4>;\n" +
            "\t.loc\t15\t109\t0\n" +
            "$LDWbegin_matrixMul_bs16_32bit:\n" +
            "\t.loc\t15\t66\t0\n" +
            "\tcvt.s32.u16 \t%r1, %ctaid.x;\n" +
            "\tmul24.lo.s32 \t%r2, %r1, 16;\n" +
            "\tcvt.s32.u16 \t%r3, %ctaid.y;\n" +
            "\tld.param.s32 \t%r4, [__cudaparm_matrixMul_bs16_32bit_wA];\n" +
            "\tmul.lo.s32 \t%r5, %r3, %r4;\n" +
            "\tmul.lo.s32 \t%r6, %r5, 16;\n" +
            "\tadd.s32 \t%r7, %r6, %r4;\n" +
            "\tsub.s32 \t%r8, %r7, 1;\n" +
            "\tcvt.s32.u16 \t%r9, %tid.x;\n" +
            "\tcvt.s32.u16 \t%r10, %tid.y;\n" +
            "\tld.param.s32 \t%r11, [__cudaparm_matrixMul_bs16_32bit_wB];\n" +
            "\tsetp.lt.s32 \t%p1, %r8, %r6;\n" +
            "\t@%p1 bra \t$Lt_0_3330;\n" +
            "\tmov.u64 \t%rd1, __cuda_local_var_16455_39_non_const_As__6;\n" +
            "\tmov.u64 \t%rd2, __cuda_local_var_16459_39_non_const_Bs__7;\n" +
            "\tld.param.s32 \t%r4, [__cudaparm_matrixMul_bs16_32bit_wA];\n" +
            "\tadd.s32 \t%r12, %r4, 15;\n" +
            "\tshr.s32 \t%r13, %r12, 31;\n" +
            "\tmov.s32 \t%r14, 15;\n" +
            "\tand.b32 \t%r15, %r13, %r14;\n" +
            "\tadd.s32 \t%r16, %r15, %r12;\n" +
            "\tshr.s32 \t%r17, %r16, 4;\n" +
            "\tld.param.s32 \t%r11, [__cudaparm_matrixMul_bs16_32bit_wB];\n" +
            "\tmul.lo.s32 \t%r18, %r10, %r11;\n" +
            "\tmul.lo.s32 \t%r19, %r10, %r4;\n" +
            "\tcvt.s64.s32 \t%rd3, %r9;\n" +
            "\tcvt.s64.s32 \t%rd4, %r10;\n" +
            "\tadd.s32 \t%r20, %r19, %r6;\n" +
            "\tadd.s32 \t%r21, %r9, %r20;\n" +
            "\tmul.wide.s32 \t%rd5, %r9, 4;\n" +
            "\tadd.u64 \t%rd6, %rd2, %rd5;\n" +
            "\tmul.wide.s32 \t%rd7, %r10, 64;\n" +
            "\tadd.u64 \t%rd8, %rd1, %rd7;\n" +
            "\tmul.wide.s32 \t%rd9, %r10, 16;\n" +
            "\tadd.u64 \t%rd10, %rd3, %rd9;\n" +
            "\tmul.lo.u64 \t%rd11, %rd10, 4;\n" +
            "\tadd.s32 \t%r22, %r19, %r8;\n" +
            "\tmul.lo.s32 \t%r23, %r11, 16;\n" +
            "\tcvt.s64.s32 \t%rd12, %r23;\n" +
            "\tmul.wide.s32 \t%rd13, %r23, 4;\n" +
            "\tadd.u64 \t%rd14, %rd11, %rd1;\n" +
            "\tadd.u64 \t%rd15, %rd11, %rd2;\n" +
            "\tadd.s32 \t%r24, %r22, %r9;\n" +
            "\tld.param.u64 \t%rd16, [__cudaparm_matrixMul_bs16_32bit_B];\n" +
            "\tadd.s32 \t%r25, %r18, %r2;\n" +
            "\tadd.s32 \t%r26, %r9, %r25;\n" +
            "\tcvt.s64.s32 \t%rd17, %r26;\n" +
            "\tmul.wide.s32 \t%rd18, %r26, 4;\n" +
            "\tadd.u64 \t%rd19, %rd16, %rd18;\n" +
            "\tld.param.u64 \t%rd20, [__cudaparm_matrixMul_bs16_32bit_A];\n" +
            "\tcvt.s64.s32 \t%rd21, %r21;\n" +
            "\tmul.wide.s32 \t%rd22, %r21, 4;\n" +
            "\tadd.u64 \t%rd23, %rd20, %rd22;\n" +
            "\tmov.f32 \t%f1, 0f00000000;     \t// 0\n" +
            "\tmov.s32 \t%r27, %r17;\n" +
            "$Lt_0_2818:\n" +
            " //<loop> Loop body line 66, nesting depth: 1, estimated iterations: unknown\n" +
            "\t.loc\t15\t82\t0\n" +
            "\tld.global.f32 \t%f2, [%rd23+0];\n" +
            "\tst.shared.f32 \t[%rd14+0], %f2;\n" +
            "\t.loc\t15\t83\t0\n" +
            "\tld.global.f32 \t%f3, [%rd19+0];\n" +
            "\tst.shared.f32 \t[%rd15+0], %f3;\n" +
            "\t.loc\t15\t86\t0\n" +
            "\tbar.sync \t0;\n" +
            "\t.loc\t15\t94\t0\n" +
            "\tld.shared.f32 \t%f4, [%rd8+0];\n" +
            "\tld.shared.f32 \t%f5, [%rd6+0];\n" +
            "\tmad.f32 \t%f6, %f4, %f5, %f1;\n" +
            "\tld.shared.f32 \t%f7, [%rd8+4];\n" +
            "\tld.shared.f32 \t%f8, [%rd6+64];\n" +
            "\tmad.f32 \t%f9, %f7, %f8, %f6;\n" +
            "\tld.shared.f32 \t%f10, [%rd8+8];\n" +
            "\tld.shared.f32 \t%f11, [%rd6+128];\n" +
            "\tmad.f32 \t%f12, %f10, %f11, %f9;\n" +
            "\tld.shared.f32 \t%f13, [%rd8+12];\n" +
            "\tld.shared.f32 \t%f14, [%rd6+192];\n" +
            "\tmad.f32 \t%f15, %f13, %f14, %f12;\n" +
            "\tld.shared.f32 \t%f16, [%rd8+16];\n" +
            "\tld.shared.f32 \t%f17, [%rd6+256];\n" +
            "\tmad.f32 \t%f18, %f16, %f17, %f15;\n" +
            "\tld.shared.f32 \t%f19, [%rd8+20];\n" +
            "\tld.shared.f32 \t%f20, [%rd6+320];\n" +
            "\tmad.f32 \t%f21, %f19, %f20, %f18;\n" +
            "\tld.shared.f32 \t%f22, [%rd8+24];\n" +
            "\tld.shared.f32 \t%f23, [%rd6+384];\n" +
            "\tmad.f32 \t%f24, %f22, %f23, %f21;\n" +
            "\tld.shared.f32 \t%f25, [%rd8+28];\n" +
            "\tld.shared.f32 \t%f26, [%rd6+448];\n" +
            "\tmad.f32 \t%f27, %f25, %f26, %f24;\n" +
            "\tld.shared.f32 \t%f28, [%rd8+32];\n" +
            "\tld.shared.f32 \t%f29, [%rd6+512];\n" +
            "\tmad.f32 \t%f30, %f28, %f29, %f27;\n" +
            "\tld.shared.f32 \t%f31, [%rd8+36];\n" +
            "\tld.shared.f32 \t%f32, [%rd6+576];\n" +
            "\tmad.f32 \t%f33, %f31, %f32, %f30;\n" +
            "\tld.shared.f32 \t%f34, [%rd8+40];\n" +
            "\tld.shared.f32 \t%f35, [%rd6+640];\n" +
            "\tmad.f32 \t%f36, %f34, %f35, %f33;\n" +
            "\tld.shared.f32 \t%f37, [%rd8+44];\n" +
            "\tld.shared.f32 \t%f38, [%rd6+704];\n" +
            "\tmad.f32 \t%f39, %f37, %f38, %f36;\n" +
            "\tld.shared.f32 \t%f40, [%rd8+48];\n" +
            "\tld.shared.f32 \t%f41, [%rd6+768];\n" +
            "\tmad.f32 \t%f42, %f40, %f41, %f39;\n" +
            "\tld.shared.f32 \t%f43, [%rd8+52];\n" +
            "\tld.shared.f32 \t%f44, [%rd6+832];\n" +
            "\tmad.f32 \t%f45, %f43, %f44, %f42;\n" +
            "\tld.shared.f32 \t%f46, [%rd8+56];\n" +
            "\tld.shared.f32 \t%f47, [%rd6+896];\n" +
            "\tmad.f32 \t%f48, %f46, %f47, %f45;\n" +
            "\tld.shared.f32 \t%f49, [%rd8+60];\n" +
            "\tld.shared.f32 \t%f50, [%rd6+960];\n" +
            "\tmad.f32 \t%f1, %f49, %f50, %f48;\n" +
            "\t.loc\t15\t99\t0\n" +
            "\tbar.sync \t0;\n" +
            "\t.loc\t15\t66\t0\n" +
            "\tadd.u64 \t%rd19, %rd13, %rd19;\n" +
            "\tadd.s32 \t%r21, %r21, 16;\n" +
            "\tadd.u64 \t%rd23, %rd23, 64;\n" +
            "\tsetp.le.s32 \t%p2, %r21, %r24;\n" +
            "\t@%p2 bra \t$Lt_0_2818;\n" +
            "\tbra.uni \t$Lt_0_2306;\n" +
            "$Lt_0_3330:\n" +
            "\tld.param.s32 \t%r11, [__cudaparm_matrixMul_bs16_32bit_wB];\n" +
            "\tmul.lo.s32 \t%r18, %r10, %r11;\n" +
            "\tmov.f32 \t%f1, 0f00000000;     \t// 0\n" +
            "$Lt_0_2306:\n" +
            "\t.loc\t15\t105\t0\n" +
            "\tld.param.u64 \t%rd24, [__cudaparm_matrixMul_bs16_32bit_C];\n" +
            "\tmul.lo.s32 \t%r28, %r11, %r3;\n" +
            "\tadd.s32 \t%r29, %r1, %r28;\n" +
            "\tmul.lo.s32 \t%r30, %r29, 16;\n" +
            "\tadd.s32 \t%r31, %r18, %r30;\n" +
            "\tadd.s32 \t%r32, %r9, %r31;\n" +
            "\tcvt.s64.s32 \t%rd25, %r32;\n" +
            "\tmul.wide.s32 \t%rd26, %r32, 4;\n" +
            "\tadd.u64 \t%rd27, %rd24, %rd26;\n" +
            "\tst.global.f32 \t[%rd27+0], %f1;\n" +
            "\t.loc\t15\t112\t0\n" +
            "\texit;\n" +
            "$LDWend_matrixMul_bs16_32bit:\n" +
            "\t} // matrixMul_bs16_32bit\n" +
            "\t.shared .align 4 .b8 __cuda_local_var_16455_39_non_const_As__4[1024];\n" +
            "\t.shared .align 4 .b8 __cuda_local_var_16459_39_non_const_Bs__5[1024];\n" +
            "\n" +
            "\t.entry matrixMul_bs16_64bit (\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs16_64bit_C,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs16_64bit_A,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs16_64bit_B,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs16_64bit_wA,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs16_64bit_wB)\n" +
            "\t{\n" +
            "\t.reg .u64 %rd<53>;\n" +
            "\t.reg .f32 %f<52>;\n" +
            "\t.reg .pred %p<4>;\n" +
            "\t.loc\t15\t113\t0\n" +
            "$LDWbegin_matrixMul_bs16_64bit:\n" +
            "\t.loc\t15\t66\t0\n" +
            "\tcvt.u64.u16 \t%rd1, %ctaid.x;\n" +
            "\tmul.lo.u64 \t%rd2, %rd1, 16;\n" +
            "\tcvt.u64.u16 \t%rd3, %ctaid.y;\n" +
            "\tld.param.u64 \t%rd4, [__cudaparm_matrixMul_bs16_64bit_wA];\n" +
            "\tmul.lo.u64 \t%rd5, %rd3, %rd4;\n" +
            "\tmul.lo.u64 \t%rd6, %rd5, 16;\n" +
            "\tmov.s64 \t%rd7, %rd6;\n" +
            "\tadd.u64 \t%rd8, %rd6, %rd4;\n" +
            "\tsub.u64 \t%rd9, %rd8, 1;\n" +
            "\tcvt.u64.u16 \t%rd10, %tid.x;\n" +
            "\tcvt.u64.u16 \t%rd11, %tid.y;\n" +
            "\tld.param.u64 \t%rd12, [__cudaparm_matrixMul_bs16_64bit_wB];\n" +
            "\tsetp.lt.u64 \t%p1, %rd9, %rd6;\n" +
            "\t@%p1 bra \t$Lt_1_3330;\n" +
            "\tmov.u64 \t%rd13, __cuda_local_var_16455_39_non_const_As__4;\n" +
            "\tmov.u64 \t%rd14, __cuda_local_var_16459_39_non_const_Bs__5;\n" +
            "\tld.param.u64 \t%rd4, [__cudaparm_matrixMul_bs16_64bit_wA];\n" +
            "\tadd.u64 \t%rd15, %rd4, 15;\n" +
            "\tshr.s64 \t%rd16, %rd15, 63;\n" +
            "\tmov.s64 \t%rd17, 15;\n" +
            "\tand.b64 \t%rd18, %rd16, %rd17;\n" +
            "\tadd.s64 \t%rd19, %rd18, %rd15;\n" +
            "\tshr.s64 \t%rd20, %rd19, 4;\n" +
            "\tld.param.u64 \t%rd12, [__cudaparm_matrixMul_bs16_64bit_wB];\n" +
            "\tmul.lo.u64 \t%rd21, %rd11, %rd12;\n" +
            "\tmul.lo.u64 \t%rd22, %rd11, 64;\n" +
            "\tadd.u64 \t%rd23, %rd13, %rd22;\n" +
            "\tmul.lo.u64 \t%rd24, %rd10, 4;\n" +
            "\tadd.u64 \t%rd25, %rd14, %rd24;\n" +
            "\tmul.lo.u64 \t%rd26, %rd11, 16;\n" +
            "\tadd.u64 \t%rd27, %rd10, %rd26;\n" +
            "\tmul.lo.u64 \t%rd28, %rd27, 4;\n" +
            "\tmul.lo.u64 \t%rd29, %rd12, 64;\n" +
            "\tadd.u64 \t%rd30, %rd28, %rd13;\n" +
            "\tadd.u64 \t%rd31, %rd28, %rd14;\n" +
            "\tld.param.u64 \t%rd32, [__cudaparm_matrixMul_bs16_64bit_B];\n" +
            "\tadd.u64 \t%rd33, %rd21, %rd2;\n" +
            "\tadd.u64 \t%rd34, %rd10, %rd33;\n" +
            "\tmul.lo.u64 \t%rd35, %rd34, 4;\n" +
            "\tadd.u64 \t%rd36, %rd32, %rd35;\n" +
            "\tld.param.u64 \t%rd37, [__cudaparm_matrixMul_bs16_64bit_A];\n" +
            "\tmul.lo.u64 \t%rd38, %rd11, %rd4;\n" +
            "\tadd.u64 \t%rd39, %rd6, %rd38;\n" +
            "\tadd.u64 \t%rd40, %rd10, %rd39;\n" +
            "\tmul.lo.u64 \t%rd41, %rd40, 4;\n" +
            "\tadd.u64 \t%rd42, %rd37, %rd41;\n" +
            "\tmov.f32 \t%f1, 0f00000000;     \t// 0\n" +
            "\tmov.s64 \t%rd43, %rd20;\n" +
            "$Lt_1_2818:\n" +
            " //<loop> Loop body line 66, nesting depth: 1, estimated iterations: unknown\n" +
            "\t.loc\t15\t82\t0\n" +
            "\tld.global.f32 \t%f2, [%rd42+0];\n" +
            "\tst.shared.f32 \t[%rd30+0], %f2;\n" +
            "\t.loc\t15\t83\t0\n" +
            "\tld.global.f32 \t%f3, [%rd36+0];\n" +
            "\tst.shared.f32 \t[%rd31+0], %f3;\n" +
            "\t.loc\t15\t86\t0\n" +
            "\tbar.sync \t0;\n" +
            "\t.loc\t15\t94\t0\n" +
            "\tld.shared.f32 \t%f4, [%rd23+0];\n" +
            "\tld.shared.f32 \t%f5, [%rd25+0];\n" +
            "\tmad.f32 \t%f6, %f4, %f5, %f1;\n" +
            "\tld.shared.f32 \t%f7, [%rd23+4];\n" +
            "\tld.shared.f32 \t%f8, [%rd25+64];\n" +
            "\tmad.f32 \t%f9, %f7, %f8, %f6;\n" +
            "\tld.shared.f32 \t%f10, [%rd23+8];\n" +
            "\tld.shared.f32 \t%f11, [%rd25+128];\n" +
            "\tmad.f32 \t%f12, %f10, %f11, %f9;\n" +
            "\tld.shared.f32 \t%f13, [%rd23+12];\n" +
            "\tld.shared.f32 \t%f14, [%rd25+192];\n" +
            "\tmad.f32 \t%f15, %f13, %f14, %f12;\n" +
            "\tld.shared.f32 \t%f16, [%rd23+16];\n" +
            "\tld.shared.f32 \t%f17, [%rd25+256];\n" +
            "\tmad.f32 \t%f18, %f16, %f17, %f15;\n" +
            "\tld.shared.f32 \t%f19, [%rd23+20];\n" +
            "\tld.shared.f32 \t%f20, [%rd25+320];\n" +
            "\tmad.f32 \t%f21, %f19, %f20, %f18;\n" +
            "\tld.shared.f32 \t%f22, [%rd23+24];\n" +
            "\tld.shared.f32 \t%f23, [%rd25+384];\n" +
            "\tmad.f32 \t%f24, %f22, %f23, %f21;\n" +
            "\tld.shared.f32 \t%f25, [%rd23+28];\n" +
            "\tld.shared.f32 \t%f26, [%rd25+448];\n" +
            "\tmad.f32 \t%f27, %f25, %f26, %f24;\n" +
            "\tld.shared.f32 \t%f28, [%rd23+32];\n" +
            "\tld.shared.f32 \t%f29, [%rd25+512];\n" +
            "\tmad.f32 \t%f30, %f28, %f29, %f27;\n" +
            "\tld.shared.f32 \t%f31, [%rd23+36];\n" +
            "\tld.shared.f32 \t%f32, [%rd25+576];\n" +
            "\tmad.f32 \t%f33, %f31, %f32, %f30;\n" +
            "\tld.shared.f32 \t%f34, [%rd23+40];\n" +
            "\tld.shared.f32 \t%f35, [%rd25+640];\n" +
            "\tmad.f32 \t%f36, %f34, %f35, %f33;\n" +
            "\tld.shared.f32 \t%f37, [%rd23+44];\n" +
            "\tld.shared.f32 \t%f38, [%rd25+704];\n" +
            "\tmad.f32 \t%f39, %f37, %f38, %f36;\n" +
            "\tld.shared.f32 \t%f40, [%rd23+48];\n" +
            "\tld.shared.f32 \t%f41, [%rd25+768];\n" +
            "\tmad.f32 \t%f42, %f40, %f41, %f39;\n" +
            "\tld.shared.f32 \t%f43, [%rd23+52];\n" +
            "\tld.shared.f32 \t%f44, [%rd25+832];\n" +
            "\tmad.f32 \t%f45, %f43, %f44, %f42;\n" +
            "\tld.shared.f32 \t%f46, [%rd23+56];\n" +
            "\tld.shared.f32 \t%f47, [%rd25+896];\n" +
            "\tmad.f32 \t%f48, %f46, %f47, %f45;\n" +
            "\tld.shared.f32 \t%f49, [%rd23+60];\n" +
            "\tld.shared.f32 \t%f50, [%rd25+960];\n" +
            "\tmad.f32 \t%f1, %f49, %f50, %f48;\n" +
            "\t.loc\t15\t99\t0\n" +
            "\tbar.sync \t0;\n" +
            "\t.loc\t15\t66\t0\n" +
            "\tadd.u64 \t%rd36, %rd29, %rd36;\n" +
            "\tadd.u64 \t%rd7, %rd7, 16;\n" +
            "\tadd.u64 \t%rd42, %rd42, 64;\n" +
            "\tsetp.ge.u64 \t%p2, %rd9, %rd7;\n" +
            "\t@%p2 bra \t$Lt_1_2818;\n" +
            "\tbra.uni \t$Lt_1_2306;\n" +
            "$Lt_1_3330:\n" +
            "\tld.param.u64 \t%rd12, [__cudaparm_matrixMul_bs16_64bit_wB];\n" +
            "\tmul.lo.u64 \t%rd21, %rd11, %rd12;\n" +
            "\tmov.f32 \t%f1, 0f00000000;     \t// 0\n" +
            "$Lt_1_2306:\n" +
            "\t.loc\t15\t105\t0\n" +
            "\tld.param.u64 \t%rd44, [__cudaparm_matrixMul_bs16_64bit_C];\n" +
            "\tmul.lo.u64 \t%rd45, %rd12, %rd3;\n" +
            "\tadd.u64 \t%rd46, %rd1, %rd45;\n" +
            "\tmul.lo.u64 \t%rd47, %rd46, 16;\n" +
            "\tadd.u64 \t%rd48, %rd21, %rd47;\n" +
            "\tadd.u64 \t%rd49, %rd10, %rd48;\n" +
            "\tmul.lo.u64 \t%rd50, %rd49, 4;\n" +
            "\tadd.u64 \t%rd51, %rd44, %rd50;\n" +
            "\tst.global.f32 \t[%rd51+0], %f1;\n" +
            "\t.loc\t15\t116\t0\n" +
            "\texit;\n" +
            "$LDWend_matrixMul_bs16_64bit:\n" +
            "\t} // matrixMul_bs16_64bit\n" +
            "\t.shared .align 4 .b8 __cuda_local_var_16455_39_non_const_As__2[4096];\n" +
            "\t.shared .align 4 .b8 __cuda_local_var_16459_39_non_const_Bs__3[4096];\n" +
            "\n" +
            "\t.entry matrixMul_bs32_32bit (\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs32_32bit_C,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs32_32bit_A,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs32_32bit_B,\n" +
            "\t\t.param .s32 __cudaparm_matrixMul_bs32_32bit_wA,\n" +
            "\t\t.param .s32 __cudaparm_matrixMul_bs32_32bit_wB)\n" +
            "\t{\n" +
            "\t.reg .u32 %r<34>;\n" +
            "\t.reg .u64 %rd<29>;\n" +
            "\t.reg .f32 %f<100>;\n" +
            "\t.reg .pred %p<4>;\n" +
            "\t.loc\t15\t117\t0\n" +
            "$LDWbegin_matrixMul_bs32_32bit:\n" +
            "\t.loc\t15\t66\t0\n" +
            "\tcvt.s32.u16 \t%r1, %ctaid.x;\n" +
            "\tmul24.lo.s32 \t%r2, %r1, 32;\n" +
            "\tcvt.s32.u16 \t%r3, %ctaid.y;\n" +
            "\tld.param.s32 \t%r4, [__cudaparm_matrixMul_bs32_32bit_wA];\n" +
            "\tmul.lo.s32 \t%r5, %r3, %r4;\n" +
            "\tmul.lo.s32 \t%r6, %r5, 32;\n" +
            "\tadd.s32 \t%r7, %r6, %r4;\n" +
            "\tsub.s32 \t%r8, %r7, 1;\n" +
            "\tcvt.s32.u16 \t%r9, %tid.x;\n" +
            "\tcvt.s32.u16 \t%r10, %tid.y;\n" +
            "\tld.param.s32 \t%r11, [__cudaparm_matrixMul_bs32_32bit_wB];\n" +
            "\tsetp.lt.s32 \t%p1, %r8, %r6;\n" +
            "\t@%p1 bra \t$Lt_2_3330;\n" +
            "\tmov.u64 \t%rd1, __cuda_local_var_16455_39_non_const_As__2;\n" +
            "\tmov.u64 \t%rd2, __cuda_local_var_16459_39_non_const_Bs__3;\n" +
            "\tld.param.s32 \t%r4, [__cudaparm_matrixMul_bs32_32bit_wA];\n" +
            "\tadd.s32 \t%r12, %r4, 31;\n" +
            "\tshr.s32 \t%r13, %r12, 31;\n" +
            "\tmov.s32 \t%r14, 31;\n" +
            "\tand.b32 \t%r15, %r13, %r14;\n" +
            "\tadd.s32 \t%r16, %r15, %r12;\n" +
            "\tshr.s32 \t%r17, %r16, 5;\n" +
            "\tld.param.s32 \t%r11, [__cudaparm_matrixMul_bs32_32bit_wB];\n" +
            "\tmul.lo.s32 \t%r18, %r10, %r11;\n" +
            "\tmul.lo.s32 \t%r19, %r10, %r4;\n" +
            "\tcvt.s64.s32 \t%rd3, %r9;\n" +
            "\tcvt.s64.s32 \t%rd4, %r10;\n" +
            "\tadd.s32 \t%r20, %r19, %r6;\n" +
            "\tadd.s32 \t%r21, %r9, %r20;\n" +
            "\tmul.wide.s32 \t%rd5, %r9, 4;\n" +
            "\tadd.u64 \t%rd6, %rd2, %rd5;\n" +
            "\tmul.wide.s32 \t%rd7, %r10, 128;\n" +
            "\tadd.u64 \t%rd8, %rd1, %rd7;\n" +
            "\tmul.wide.s32 \t%rd9, %r10, 32;\n" +
            "\tadd.u64 \t%rd10, %rd3, %rd9;\n" +
            "\tmul.lo.u64 \t%rd11, %rd10, 4;\n" +
            "\tadd.s32 \t%r22, %r19, %r8;\n" +
            "\tmul.lo.s32 \t%r23, %r11, 32;\n" +
            "\tcvt.s64.s32 \t%rd12, %r23;\n" +
            "\tmul.wide.s32 \t%rd13, %r23, 4;\n" +
            "\tadd.u64 \t%rd14, %rd11, %rd1;\n" +
            "\tadd.u64 \t%rd15, %rd11, %rd2;\n" +
            "\tadd.s32 \t%r24, %r22, %r9;\n" +
            "\tld.param.u64 \t%rd16, [__cudaparm_matrixMul_bs32_32bit_B];\n" +
            "\tadd.s32 \t%r25, %r18, %r2;\n" +
            "\tadd.s32 \t%r26, %r9, %r25;\n" +
            "\tcvt.s64.s32 \t%rd17, %r26;\n" +
            "\tmul.wide.s32 \t%rd18, %r26, 4;\n" +
            "\tadd.u64 \t%rd19, %rd16, %rd18;\n" +
            "\tld.param.u64 \t%rd20, [__cudaparm_matrixMul_bs32_32bit_A];\n" +
            "\tcvt.s64.s32 \t%rd21, %r21;\n" +
            "\tmul.wide.s32 \t%rd22, %r21, 4;\n" +
            "\tadd.u64 \t%rd23, %rd20, %rd22;\n" +
            "\tmov.f32 \t%f1, 0f00000000;     \t// 0\n" +
            "\tmov.s32 \t%r27, %r17;\n" +
            "$Lt_2_2818:\n" +
            " //<loop> Loop body line 66, nesting depth: 1, estimated iterations: unknown\n" +
            "\t.loc\t15\t82\t0\n" +
            "\tld.global.f32 \t%f2, [%rd23+0];\n" +
            "\tst.shared.f32 \t[%rd14+0], %f2;\n" +
            "\t.loc\t15\t83\t0\n" +
            "\tld.global.f32 \t%f3, [%rd19+0];\n" +
            "\tst.shared.f32 \t[%rd15+0], %f3;\n" +
            "\t.loc\t15\t86\t0\n" +
            "\tbar.sync \t0;\n" +
            "\t.loc\t15\t94\t0\n" +
            "\tld.shared.f32 \t%f4, [%rd8+0];\n" +
            "\tld.shared.f32 \t%f5, [%rd6+0];\n" +
            "\tmad.f32 \t%f6, %f4, %f5, %f1;\n" +
            "\tld.shared.f32 \t%f7, [%rd8+4];\n" +
            "\tld.shared.f32 \t%f8, [%rd6+128];\n" +
            "\tmad.f32 \t%f9, %f7, %f8, %f6;\n" +
            "\tld.shared.f32 \t%f10, [%rd8+8];\n" +
            "\tld.shared.f32 \t%f11, [%rd6+256];\n" +
            "\tmad.f32 \t%f12, %f10, %f11, %f9;\n" +
            "\tld.shared.f32 \t%f13, [%rd8+12];\n" +
            "\tld.shared.f32 \t%f14, [%rd6+384];\n" +
            "\tmad.f32 \t%f15, %f13, %f14, %f12;\n" +
            "\tld.shared.f32 \t%f16, [%rd8+16];\n" +
            "\tld.shared.f32 \t%f17, [%rd6+512];\n" +
            "\tmad.f32 \t%f18, %f16, %f17, %f15;\n" +
            "\tld.shared.f32 \t%f19, [%rd8+20];\n" +
            "\tld.shared.f32 \t%f20, [%rd6+640];\n" +
            "\tmad.f32 \t%f21, %f19, %f20, %f18;\n" +
            "\tld.shared.f32 \t%f22, [%rd8+24];\n" +
            "\tld.shared.f32 \t%f23, [%rd6+768];\n" +
            "\tmad.f32 \t%f24, %f22, %f23, %f21;\n" +
            "\tld.shared.f32 \t%f25, [%rd8+28];\n" +
            "\tld.shared.f32 \t%f26, [%rd6+896];\n" +
            "\tmad.f32 \t%f27, %f25, %f26, %f24;\n" +
            "\tld.shared.f32 \t%f28, [%rd8+32];\n" +
            "\tld.shared.f32 \t%f29, [%rd6+1024];\n" +
            "\tmad.f32 \t%f30, %f28, %f29, %f27;\n" +
            "\tld.shared.f32 \t%f31, [%rd8+36];\n" +
            "\tld.shared.f32 \t%f32, [%rd6+1152];\n" +
            "\tmad.f32 \t%f33, %f31, %f32, %f30;\n" +
            "\tld.shared.f32 \t%f34, [%rd8+40];\n" +
            "\tld.shared.f32 \t%f35, [%rd6+1280];\n" +
            "\tmad.f32 \t%f36, %f34, %f35, %f33;\n" +
            "\tld.shared.f32 \t%f37, [%rd8+44];\n" +
            "\tld.shared.f32 \t%f38, [%rd6+1408];\n" +
            "\tmad.f32 \t%f39, %f37, %f38, %f36;\n" +
            "\tld.shared.f32 \t%f40, [%rd8+48];\n" +
            "\tld.shared.f32 \t%f41, [%rd6+1536];\n" +
            "\tmad.f32 \t%f42, %f40, %f41, %f39;\n" +
            "\tld.shared.f32 \t%f43, [%rd8+52];\n" +
            "\tld.shared.f32 \t%f44, [%rd6+1664];\n" +
            "\tmad.f32 \t%f45, %f43, %f44, %f42;\n" +
            "\tld.shared.f32 \t%f46, [%rd8+56];\n" +
            "\tld.shared.f32 \t%f47, [%rd6+1792];\n" +
            "\tmad.f32 \t%f48, %f46, %f47, %f45;\n" +
            "\tld.shared.f32 \t%f49, [%rd8+60];\n" +
            "\tld.shared.f32 \t%f50, [%rd6+1920];\n" +
            "\tmad.f32 \t%f51, %f49, %f50, %f48;\n" +
            "\tld.shared.f32 \t%f52, [%rd8+64];\n" +
            "\tld.shared.f32 \t%f53, [%rd6+2048];\n" +
            "\tmad.f32 \t%f54, %f52, %f53, %f51;\n" +
            "\tld.shared.f32 \t%f55, [%rd8+68];\n" +
            "\tld.shared.f32 \t%f56, [%rd6+2176];\n" +
            "\tmad.f32 \t%f57, %f55, %f56, %f54;\n" +
            "\tld.shared.f32 \t%f58, [%rd8+72];\n" +
            "\tld.shared.f32 \t%f59, [%rd6+2304];\n" +
            "\tmad.f32 \t%f60, %f58, %f59, %f57;\n" +
            "\tld.shared.f32 \t%f61, [%rd8+76];\n" +
            "\tld.shared.f32 \t%f62, [%rd6+2432];\n" +
            "\tmad.f32 \t%f63, %f61, %f62, %f60;\n" +
            "\tld.shared.f32 \t%f64, [%rd8+80];\n" +
            "\tld.shared.f32 \t%f65, [%rd6+2560];\n" +
            "\tmad.f32 \t%f66, %f64, %f65, %f63;\n" +
            "\tld.shared.f32 \t%f67, [%rd8+84];\n" +
            "\tld.shared.f32 \t%f68, [%rd6+2688];\n" +
            "\tmad.f32 \t%f69, %f67, %f68, %f66;\n" +
            "\tld.shared.f32 \t%f70, [%rd8+88];\n" +
            "\tld.shared.f32 \t%f71, [%rd6+2816];\n" +
            "\tmad.f32 \t%f72, %f70, %f71, %f69;\n" +
            "\tld.shared.f32 \t%f73, [%rd8+92];\n" +
            "\tld.shared.f32 \t%f74, [%rd6+2944];\n" +
            "\tmad.f32 \t%f75, %f73, %f74, %f72;\n" +
            "\tld.shared.f32 \t%f76, [%rd8+96];\n" +
            "\tld.shared.f32 \t%f77, [%rd6+3072];\n" +
            "\tmad.f32 \t%f78, %f76, %f77, %f75;\n" +
            "\tld.shared.f32 \t%f79, [%rd8+100];\n" +
            "\tld.shared.f32 \t%f80, [%rd6+3200];\n" +
            "\tmad.f32 \t%f81, %f79, %f80, %f78;\n" +
            "\tld.shared.f32 \t%f82, [%rd8+104];\n" +
            "\tld.shared.f32 \t%f83, [%rd6+3328];\n" +
            "\tmad.f32 \t%f84, %f82, %f83, %f81;\n" +
            "\tld.shared.f32 \t%f85, [%rd8+108];\n" +
            "\tld.shared.f32 \t%f86, [%rd6+3456];\n" +
            "\tmad.f32 \t%f87, %f85, %f86, %f84;\n" +
            "\tld.shared.f32 \t%f88, [%rd8+112];\n" +
            "\tld.shared.f32 \t%f89, [%rd6+3584];\n" +
            "\tmad.f32 \t%f90, %f88, %f89, %f87;\n" +
            "\tld.shared.f32 \t%f91, [%rd8+116];\n" +
            "\tld.shared.f32 \t%f92, [%rd6+3712];\n" +
            "\tmad.f32 \t%f93, %f91, %f92, %f90;\n" +
            "\tld.shared.f32 \t%f94, [%rd8+120];\n" +
            "\tld.shared.f32 \t%f95, [%rd6+3840];\n" +
            "\tmad.f32 \t%f96, %f94, %f95, %f93;\n" +
            "\tld.shared.f32 \t%f97, [%rd8+124];\n" +
            "\tld.shared.f32 \t%f98, [%rd6+3968];\n" +
            "\tmad.f32 \t%f1, %f97, %f98, %f96;\n" +
            "\t.loc\t15\t99\t0\n" +
            "\tbar.sync \t0;\n" +
            "\t.loc\t15\t66\t0\n" +
            "\tadd.u64 \t%rd19, %rd13, %rd19;\n" +
            "\tadd.s32 \t%r21, %r21, 32;\n" +
            "\tadd.u64 \t%rd23, %rd23, 128;\n" +
            "\tsetp.le.s32 \t%p2, %r21, %r24;\n" +
            "\t@%p2 bra \t$Lt_2_2818;\n" +
            "\tbra.uni \t$Lt_2_2306;\n" +
            "$Lt_2_3330:\n" +
            "\tld.param.s32 \t%r11, [__cudaparm_matrixMul_bs32_32bit_wB];\n" +
            "\tmul.lo.s32 \t%r18, %r10, %r11;\n" +
            "\tmov.f32 \t%f1, 0f00000000;     \t// 0\n" +
            "$Lt_2_2306:\n" +
            "\t.loc\t15\t105\t0\n" +
            "\tld.param.u64 \t%rd24, [__cudaparm_matrixMul_bs32_32bit_C];\n" +
            "\tmul.lo.s32 \t%r28, %r11, %r3;\n" +
            "\tadd.s32 \t%r29, %r1, %r28;\n" +
            "\tmul.lo.s32 \t%r30, %r29, 32;\n" +
            "\tadd.s32 \t%r31, %r18, %r30;\n" +
            "\tadd.s32 \t%r32, %r9, %r31;\n" +
            "\tcvt.s64.s32 \t%rd25, %r32;\n" +
            "\tmul.wide.s32 \t%rd26, %r32, 4;\n" +
            "\tadd.u64 \t%rd27, %rd24, %rd26;\n" +
            "\tst.global.f32 \t[%rd27+0], %f1;\n" +
            "\t.loc\t15\t120\t0\n" +
            "\texit;\n" +
            "$LDWend_matrixMul_bs32_32bit:\n" +
            "\t} // matrixMul_bs32_32bit\n" +
            "\t.shared .align 4 .b8 __cuda_local_var_16455_39_non_const_As__0[4096];\n" +
            "\t.shared .align 4 .b8 __cuda_local_var_16459_39_non_const_Bs__1[4096];\n" +
            "\n" +
            "\t.entry matrixMul_bs32_64bit (\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs32_64bit_C,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs32_64bit_A,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs32_64bit_B,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs32_64bit_wA,\n" +
            "\t\t.param .u64 __cudaparm_matrixMul_bs32_64bit_wB)\n" +
            "\t{\n" +
            "\t.reg .u64 %rd<53>;\n" +
            "\t.reg .f32 %f<100>;\n" +
            "\t.reg .pred %p<4>;\n" +
            "\t.loc\t15\t121\t0\n" +
            "$LDWbegin_matrixMul_bs32_64bit:\n" +
            "\t.loc\t15\t66\t0\n" +
            "\tcvt.u64.u16 \t%rd1, %ctaid.x;\n" +
            "\tmul.lo.u64 \t%rd2, %rd1, 32;\n" +
            "\tcvt.u64.u16 \t%rd3, %ctaid.y;\n" +
            "\tld.param.u64 \t%rd4, [__cudaparm_matrixMul_bs32_64bit_wA];\n" +
            "\tmul.lo.u64 \t%rd5, %rd3, %rd4;\n" +
            "\tmul.lo.u64 \t%rd6, %rd5, 32;\n" +
            "\tmov.s64 \t%rd7, %rd6;\n" +
            "\tadd.u64 \t%rd8, %rd6, %rd4;\n" +
            "\tsub.u64 \t%rd9, %rd8, 1;\n" +
            "\tcvt.u64.u16 \t%rd10, %tid.x;\n" +
            "\tcvt.u64.u16 \t%rd11, %tid.y;\n" +
            "\tld.param.u64 \t%rd12, [__cudaparm_matrixMul_bs32_64bit_wB];\n" +
            "\tsetp.lt.u64 \t%p1, %rd9, %rd6;\n" +
            "\t@%p1 bra \t$Lt_3_3330;\n" +
            "\tmov.u64 \t%rd13, __cuda_local_var_16455_39_non_const_As__0;\n" +
            "\tmov.u64 \t%rd14, __cuda_local_var_16459_39_non_const_Bs__1;\n" +
            "\tld.param.u64 \t%rd4, [__cudaparm_matrixMul_bs32_64bit_wA];\n" +
            "\tadd.u64 \t%rd15, %rd4, 31;\n" +
            "\tshr.s64 \t%rd16, %rd15, 63;\n" +
            "\tmov.s64 \t%rd17, 31;\n" +
            "\tand.b64 \t%rd18, %rd16, %rd17;\n" +
            "\tadd.s64 \t%rd19, %rd18, %rd15;\n" +
            "\tshr.s64 \t%rd20, %rd19, 5;\n" +
            "\tld.param.u64 \t%rd12, [__cudaparm_matrixMul_bs32_64bit_wB];\n" +
            "\tmul.lo.u64 \t%rd21, %rd11, %rd12;\n" +
            "\tmul.lo.u64 \t%rd22, %rd11, 128;\n" +
            "\tadd.u64 \t%rd23, %rd13, %rd22;\n" +
            "\tmul.lo.u64 \t%rd24, %rd10, 4;\n" +
            "\tadd.u64 \t%rd25, %rd14, %rd24;\n" +
            "\tmul.lo.u64 \t%rd26, %rd11, 32;\n" +
            "\tadd.u64 \t%rd27, %rd10, %rd26;\n" +
            "\tmul.lo.u64 \t%rd28, %rd27, 4;\n" +
            "\tmul.lo.u64 \t%rd29, %rd12, 128;\n" +
            "\tadd.u64 \t%rd30, %rd28, %rd13;\n" +
            "\tadd.u64 \t%rd31, %rd28, %rd14;\n" +
            "\tld.param.u64 \t%rd32, [__cudaparm_matrixMul_bs32_64bit_B];\n" +
            "\tadd.u64 \t%rd33, %rd21, %rd2;\n" +
            "\tadd.u64 \t%rd34, %rd10, %rd33;\n" +
            "\tmul.lo.u64 \t%rd35, %rd34, 4;\n" +
            "\tadd.u64 \t%rd36, %rd32, %rd35;\n" +
            "\tld.param.u64 \t%rd37, [__cudaparm_matrixMul_bs32_64bit_A];\n" +
            "\tmul.lo.u64 \t%rd38, %rd11, %rd4;\n" +
            "\tadd.u64 \t%rd39, %rd6, %rd38;\n" +
            "\tadd.u64 \t%rd40, %rd10, %rd39;\n" +
            "\tmul.lo.u64 \t%rd41, %rd40, 4;\n" +
            "\tadd.u64 \t%rd42, %rd37, %rd41;\n" +
            "\tmov.f32 \t%f1, 0f00000000;     \t// 0\n" +
            "\tmov.s64 \t%rd43, %rd20;\n" +
            "$Lt_3_2818:\n" +
            " //<loop> Loop body line 66, nesting depth: 1, estimated iterations: unknown\n" +
            "\t.loc\t15\t82\t0\n" +
            "\tld.global.f32 \t%f2, [%rd42+0];\n" +
            "\tst.shared.f32 \t[%rd30+0], %f2;\n" +
            "\t.loc\t15\t83\t0\n" +
            "\tld.global.f32 \t%f3, [%rd36+0];\n" +
            "\tst.shared.f32 \t[%rd31+0], %f3;\n" +
            "\t.loc\t15\t86\t0\n" +
            "\tbar.sync \t0;\n" +
            "\t.loc\t15\t94\t0\n" +
            "\tld.shared.f32 \t%f4, [%rd23+0];\n" +
            "\tld.shared.f32 \t%f5, [%rd25+0];\n" +
            "\tmad.f32 \t%f6, %f4, %f5, %f1;\n" +
            "\tld.shared.f32 \t%f7, [%rd23+4];\n" +
            "\tld.shared.f32 \t%f8, [%rd25+128];\n" +
            "\tmad.f32 \t%f9, %f7, %f8, %f6;\n" +
            "\tld.shared.f32 \t%f10, [%rd23+8];\n" +
            "\tld.shared.f32 \t%f11, [%rd25+256];\n" +
            "\tmad.f32 \t%f12, %f10, %f11, %f9;\n" +
            "\tld.shared.f32 \t%f13, [%rd23+12];\n" +
            "\tld.shared.f32 \t%f14, [%rd25+384];\n" +
            "\tmad.f32 \t%f15, %f13, %f14, %f12;\n" +
            "\tld.shared.f32 \t%f16, [%rd23+16];\n" +
            "\tld.shared.f32 \t%f17, [%rd25+512];\n" +
            "\tmad.f32 \t%f18, %f16, %f17, %f15;\n" +
            "\tld.shared.f32 \t%f19, [%rd23+20];\n" +
            "\tld.shared.f32 \t%f20, [%rd25+640];\n" +
            "\tmad.f32 \t%f21, %f19, %f20, %f18;\n" +
            "\tld.shared.f32 \t%f22, [%rd23+24];\n" +
            "\tld.shared.f32 \t%f23, [%rd25+768];\n" +
            "\tmad.f32 \t%f24, %f22, %f23, %f21;\n" +
            "\tld.shared.f32 \t%f25, [%rd23+28];\n" +
            "\tld.shared.f32 \t%f26, [%rd25+896];\n" +
            "\tmad.f32 \t%f27, %f25, %f26, %f24;\n" +
            "\tld.shared.f32 \t%f28, [%rd23+32];\n" +
            "\tld.shared.f32 \t%f29, [%rd25+1024];\n" +
            "\tmad.f32 \t%f30, %f28, %f29, %f27;\n" +
            "\tld.shared.f32 \t%f31, [%rd23+36];\n" +
            "\tld.shared.f32 \t%f32, [%rd25+1152];\n" +
            "\tmad.f32 \t%f33, %f31, %f32, %f30;\n" +
            "\tld.shared.f32 \t%f34, [%rd23+40];\n" +
            "\tld.shared.f32 \t%f35, [%rd25+1280];\n" +
            "\tmad.f32 \t%f36, %f34, %f35, %f33;\n" +
            "\tld.shared.f32 \t%f37, [%rd23+44];\n" +
            "\tld.shared.f32 \t%f38, [%rd25+1408];\n" +
            "\tmad.f32 \t%f39, %f37, %f38, %f36;\n" +
            "\tld.shared.f32 \t%f40, [%rd23+48];\n" +
            "\tld.shared.f32 \t%f41, [%rd25+1536];\n" +
            "\tmad.f32 \t%f42, %f40, %f41, %f39;\n" +
            "\tld.shared.f32 \t%f43, [%rd23+52];\n" +
            "\tld.shared.f32 \t%f44, [%rd25+1664];\n" +
            "\tmad.f32 \t%f45, %f43, %f44, %f42;\n" +
            "\tld.shared.f32 \t%f46, [%rd23+56];\n" +
            "\tld.shared.f32 \t%f47, [%rd25+1792];\n" +
            "\tmad.f32 \t%f48, %f46, %f47, %f45;\n" +
            "\tld.shared.f32 \t%f49, [%rd23+60];\n" +
            "\tld.shared.f32 \t%f50, [%rd25+1920];\n" +
            "\tmad.f32 \t%f51, %f49, %f50, %f48;\n" +
            "\tld.shared.f32 \t%f52, [%rd23+64];\n" +
            "\tld.shared.f32 \t%f53, [%rd25+2048];\n" +
            "\tmad.f32 \t%f54, %f52, %f53, %f51;\n" +
            "\tld.shared.f32 \t%f55, [%rd23+68];\n" +
            "\tld.shared.f32 \t%f56, [%rd25+2176];\n" +
            "\tmad.f32 \t%f57, %f55, %f56, %f54;\n" +
            "\tld.shared.f32 \t%f58, [%rd23+72];\n" +
            "\tld.shared.f32 \t%f59, [%rd25+2304];\n" +
            "\tmad.f32 \t%f60, %f58, %f59, %f57;\n" +
            "\tld.shared.f32 \t%f61, [%rd23+76];\n" +
            "\tld.shared.f32 \t%f62, [%rd25+2432];\n" +
            "\tmad.f32 \t%f63, %f61, %f62, %f60;\n" +
            "\tld.shared.f32 \t%f64, [%rd23+80];\n" +
            "\tld.shared.f32 \t%f65, [%rd25+2560];\n" +
            "\tmad.f32 \t%f66, %f64, %f65, %f63;\n" +
            "\tld.shared.f32 \t%f67, [%rd23+84];\n" +
            "\tld.shared.f32 \t%f68, [%rd25+2688];\n" +
            "\tmad.f32 \t%f69, %f67, %f68, %f66;\n" +
            "\tld.shared.f32 \t%f70, [%rd23+88];\n" +
            "\tld.shared.f32 \t%f71, [%rd25+2816];\n" +
            "\tmad.f32 \t%f72, %f70, %f71, %f69;\n" +
            "\tld.shared.f32 \t%f73, [%rd23+92];\n" +
            "\tld.shared.f32 \t%f74, [%rd25+2944];\n" +
            "\tmad.f32 \t%f75, %f73, %f74, %f72;\n" +
            "\tld.shared.f32 \t%f76, [%rd23+96];\n" +
            "\tld.shared.f32 \t%f77, [%rd25+3072];\n" +
            "\tmad.f32 \t%f78, %f76, %f77, %f75;\n" +
            "\tld.shared.f32 \t%f79, [%rd23+100];\n" +
            "\tld.shared.f32 \t%f80, [%rd25+3200];\n" +
            "\tmad.f32 \t%f81, %f79, %f80, %f78;\n" +
            "\tld.shared.f32 \t%f82, [%rd23+104];\n" +
            "\tld.shared.f32 \t%f83, [%rd25+3328];\n" +
            "\tmad.f32 \t%f84, %f82, %f83, %f81;\n" +
            "\tld.shared.f32 \t%f85, [%rd23+108];\n" +
            "\tld.shared.f32 \t%f86, [%rd25+3456];\n" +
            "\tmad.f32 \t%f87, %f85, %f86, %f84;\n" +
            "\tld.shared.f32 \t%f88, [%rd23+112];\n" +
            "\tld.shared.f32 \t%f89, [%rd25+3584];\n" +
            "\tmad.f32 \t%f90, %f88, %f89, %f87;\n" +
            "\tld.shared.f32 \t%f91, [%rd23+116];\n" +
            "\tld.shared.f32 \t%f92, [%rd25+3712];\n" +
            "\tmad.f32 \t%f93, %f91, %f92, %f90;\n" +
            "\tld.shared.f32 \t%f94, [%rd23+120];\n" +
            "\tld.shared.f32 \t%f95, [%rd25+3840];\n" +
            "\tmad.f32 \t%f96, %f94, %f95, %f93;\n" +
            "\tld.shared.f32 \t%f97, [%rd23+124];\n" +
            "\tld.shared.f32 \t%f98, [%rd25+3968];\n" +
            "\tmad.f32 \t%f1, %f97, %f98, %f96;\n" +
            "\t.loc\t15\t99\t0\n" +
            "\tbar.sync \t0;\n" +
            "\t.loc\t15\t66\t0\n" +
            "\tadd.u64 \t%rd36, %rd29, %rd36;\n" +
            "\tadd.u64 \t%rd7, %rd7, 32;\n" +
            "\tadd.u64 \t%rd42, %rd42, 128;\n" +
            "\tsetp.ge.u64 \t%p2, %rd9, %rd7;\n" +
            "\t@%p2 bra \t$Lt_3_2818;\n" +
            "\tbra.uni \t$Lt_3_2306;\n" +
            "$Lt_3_3330:\n" +
            "\tld.param.u64 \t%rd12, [__cudaparm_matrixMul_bs32_64bit_wB];\n" +
            "\tmul.lo.u64 \t%rd21, %rd11, %rd12;\n" +
            "\tmov.f32 \t%f1, 0f00000000;     \t// 0\n" +
            "$Lt_3_2306:\n" +
            "\t.loc\t15\t105\t0\n" +
            "\tld.param.u64 \t%rd44, [__cudaparm_matrixMul_bs32_64bit_C];\n" +
            "\tmul.lo.u64 \t%rd45, %rd12, %rd3;\n" +
            "\tadd.u64 \t%rd46, %rd1, %rd45;\n" +
            "\tmul.lo.u64 \t%rd47, %rd46, 32;\n" +
            "\tadd.u64 \t%rd48, %rd21, %rd47;\n" +
            "\tadd.u64 \t%rd49, %rd10, %rd48;\n" +
            "\tmul.lo.u64 \t%rd50, %rd49, 4;\n" +
            "\tadd.u64 \t%rd51, %rd44, %rd50;\n" +
            "\tst.global.f32 \t[%rd51+0], %f1;\n" +
            "\t.loc\t15\t124\t0\n" +
            "\texit;\n" +
            "$LDWend_matrixMul_bs32_64bit:\n" +
            "\t} // matrixMul_bs32_64bit\n" +
            "\n";
}
