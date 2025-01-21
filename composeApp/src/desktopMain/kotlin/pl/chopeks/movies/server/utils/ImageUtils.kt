package pl.chopeks.movies.server.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

val videoExtensions = arrayOf("264", "3g2", "3gp", "3gp2", "3gpp", "3gpp2", "3mm", "3p2", "60d", "787", "890", "aaf", "aec", "aecap", "aegraphic", "aep", "aepx", "aet", "aetx", "ajp", "ale", "am", "amc", "amv", "amx", "anim", "anx", "aqt", "arcut", "arf", "asf", "asx", "avb", "avc", "avchd", "avd", "avi", "avm", "avp", "avs", "avs", "avv", "awlive", "axm", "axv", "bdm", "bdmv", "bdt2", "bdt3", "bik", "bin", "bix", "bmc", "bmk", "bnp", "box", "bs4", "bsf", "bu", "bvr", "byu", "camproj", "camrec", "camv", "ced", "cel", "cine", "cip", "clk", "clpi", "cmmp", "cmmtpl", "cmproj", "cmrec", "cmv", "cpi", "cpvc", "crec", "cst", "cvc", "cx3", "d2v", "d3v", "dad", "dash", "dat", "dav", "db2", "dce", "dck", "dcr", "dcr", "ddat", "dif", "dir", "divx", "dlx", "dmb", "dmsd", "dmsd3d", "dmsm", "dmsm3d", "dmss", "dmx", "dnc", "dpa", "dpg", "dream", "dsy", "dv", "dv-avi", "dv4", "dvdmedia", "dvr", "dvr-ms", "dvx", "dxr", "dzm", "dzp", "dzt", "edl", "evo", "evo", "exo", "eye", "eyetv", "ezt", "f4f", "f4m", "f4p", "f4v", "fbr", "fbr", "fbz", "fcarch", "fcp", "fcproject", "ffd", "ffm", "flc", "flh", "fli", "flic", "flv", "flx", "fpdx", "ftc", "fvt", "g2m", "g64", "g64x", "gcs", "gfp", "gifv", "gl", "gom", "grasp", "gts", "gvi", "gvp", "gxf", "h264", "hdmov", "hdv", "hkm", "ifo", "imovielibrary", "imoviemobile", "imovieproj", "imovieproject", "inp", "int", "ircp", "irf", "ism", "ismc", "ismclip", "ismv", "iva", "ivf", "ivr", "ivs", "izz", "izzy", "jdr", "jmv", "jnr", "jss", "jts", "jtv", "k3g", "kdenlive", "kmv", "ktn", "lrec", "lrv", "lsf", "lsx", "lvix", "m15", "m1pg", "m1v", "m21", "m21", "m2a", "m2p", "m2t", "m2ts", "m2v", "m4e", "m4u", "m4v", "m75", "mani", "meta", "mgv", "mj2", "mjp", "mjpeg", "mjpg", "mk3d", "mkv", "mmv", "mnv", "mob", "mod", "modd", "moff", "moi", "moov", "mov", "movie", "mp21", "mp21", "mp2v", "mp4", "mp4infovid", "mp4v", "mpe", "mpeg", "mpeg1", "mpeg2", "mpeg4", "mpf", "mpg", "mpg2", "mpg4", "mpgindex", "mpl", "mpl", "mpls", "mproj", "mpsub", "mpv", "mpv2", "mqv", "msdvd", "mse", "msh", "mswmm", "mt2s", "mts", "mtv", "mvb", "mvc", "mvd", "mve", "mvex", "mvp", "mvp", "mvy", "mxf", "mxv", "mys", "n3r", "ncor", "nfv", "nsv", "ntp", "nut", "nuv", "nvc", "ogm", "ogv", "ogx", "orv", "osp", "otrkey", "pac", "par", "pds", "pgi", "photoshow", "piv", "pjs", "playlist", "plproj", "pmf", "pmv", "pns", "ppj", "prel", "pro", "pro4dvd", "pro5dvd", "proqc", "prproj", "prtl", "psb", "psh", "pssd", "psv", "pva", "pvr", "pxv", "pz", "qt", "qtch", "qtindex", "qtl", "qtm", "qtz", "r3d", "rcd", "rcproject", "rcrec", "rcut", "rdb", "rec", "rm", "rmd", "rmd", "rmp", "rms", "rmv", "rmvb", "roq", "rp", "rsx", "rts", "rts", "rum", "rv", "rvid", "rvl", "san", "sbk", "sbt", "sbz", "scc", "scm", "scm", "scn", "screenflow", "sdv", "sec", "sec", "sedprj", "seq", "sfd", "sfera", "sfvidcap", "siv", "smi", "smi", "smil", "smk", "sml", "smv", "snagproj", "spl", "sqz", "srt", "ssf", "ssm", "stl", "str", "stx", "svi", "swf", "swi", "swt", "tda3mt", "tdt", "tdx", "theater", "thp", "tid", "tivo", "tix", "tod", "tp", "tp0", "tpd", "tpr", "trec", "trp", "ts", "tsp", "ttxt", "tvlayer", "tvrecording", "tvs", "tvshow", "usf", "usm", "v264", "vbc", "vc1", "vcpf", "vcr", "vcv", "vdo", "vdr", "vdx", "veg", "vem", "vep", "vf", "vft", "vfw", "vfz", "vgz", "vid", "video", "viewlet", "viv", "vivo", "vix", "vlab", "vmlf", "vmlt", "vob", "vp3", "vp6", "vp7", "vpj", "vr", "vro", "vs4", "vse", "vsp", "vtt", "w32", "wcp", "webm", "wfsp", "wgi", "wlmp", "wm", "wmd", "wmmp", "wmv", "wmx", "wot", "wp3", "wpl", "wsve", "wtv", "wve", "wvm", "wvx", "wxp", "xej", "xel", "xesc", "xfl", "xlmv", "xml", "xmv", "xvid", "y4m", "yog", "yuv", "zeg", "zm1", "zm2", "zm3", "zmv")

fun getFiles(dir: File) = dir.walkTopDown()
  .filter { it.isFile }
  .filter { it.extension in videoExtensions }
  .toCollection(mutableListOf())

fun ClosedRange<Int>.random() =
  Random.Default.nextInt(endInclusive - start) + start

fun BufferedImage.normalizeImage() = this
  .let { Scalr.resize(it, Scalr.Method.QUALITY, 400, it.height * 400 / it.width) }
  .let {
    val newHeight = minOf(it.width * 9 / 16, it.height)
    val offset = (it.height - newHeight) / 2
    Scalr.crop(it, 0, offset, it.width, newHeight)
  }

@OptIn(ExperimentalEncodingApi::class)
fun String.urlImageToBase64(): String =
  "data:image/jpg;base64," + OkHttpClient()
    .newCall(Request.Builder()
      .url(this)
      .build()
    )
    .execute()
    .body
    ?.bytes()
    ?.let(Base64.Mime::encode)

