import org.ergoplatform.ErgoBox.R7
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.ergoplatform.appkit.{Address, BlockchainContext, ConstantsBuilder, ErgoClient, ErgoProver, ErgoType, ErgoValue, InputBox, NetworkType, OutBox, RestApiErgoClient, TransactionBox}
import org.ergoplatform.sdk.{ErgoId, ErgoToken, Iso, JavaHelpers}
import scorex.crypto.hash.Blake2b256
import sigmastate.Values.ErgoTree
import sigmastate.eval.Colls
import special.collection.{Coll, collBuilderRType}

import scala.math.BigDecimal.long2bigDecimal
import scala.collection.JavaConverters._

object Main {
//  val ergoClient: ErgoClient = RestApiErgoClient.create("http://128.140.55.130:9053/", NetworkType.MAINNET, "", "https://api.ergoplatform.com/api/v1/")
  val ergoClient: ErgoClient = RestApiErgoClient.create("http://95.217.180.19:9053/", NetworkType.MAINNET, "", "https://api.ergoplatform.com/api/v1/")
  val addrEnc = new ErgoAddressEncoder(NetworkType.MAINNET.networkPrefix)

  // for testing
  val USER_P2PK_ADDRESS: ErgoAddress = Address.create("9gN7YssQgnRzDDXHAuamAwgikRkzeLYq8Jn9YvsPcKD13ajj33u").getErgoAddress
  val USER_SECRET = "19899857734851286010002701428515536889175392016268936110507684368097105859366"

  var buyBackAddr: ErgoAddress = _
  var acAddr: ErgoAddress = _

  val acId = "52f4544ce8a420d484ece16f9b984d81c23e46971ef5e37c29382ac50f80d5bd"
  val teamAddr = addrEnc.fromString("9gy7nr9ReQ7K26uaUHHebACA7qURGyPxu6RKLaPFCHNbGAq4BdZ").get
  val acNFT = ErgoId.create("f457187facd8509320f0fe32c72b491904c96f3b1059b66705849b9da1b8d54c")
  val auctionAddr = addrEnc.fromString("quE2HvwgAp7z8q4tD1BfCfj72TPziUHqruk3L8jUWQ7bWGD7ugdkpkgYx9kztPTmJborN4baTrHuenaZrCtoB3VHeLjQUQACzZ67qpnMGUAyGs93mUpwodKekvSM8Y6rCFVMxFw76MD8ZUjMPd2J8awcfECurL9v6HkjFnzKJr72NXdQ18XYYGRneXbbkgU5qzUKTLoC2weUGsDpNhfDh9xBGV7CYconikP79c9b3ZcquSBdNCu4G5r2uvBVTHXaFgk5TioeSL4SqwS5sugeEeicTPre842uZBzNd7B3XqE75Rjum1bz1vBgGtadMqt2XRui7tqaK44LjeYfds3N4CWZsYwx69h2PpHFrU9Mzvvj3ju6XGVS5X6GnyGgttXiHgokZPhD45m2etzAUXovNPeutrw5zmQudCh9jXBSfujXDBPS7ohC7qLj3Hj4zfRJTmUzPNMEUGLED57kowMXdAq95h4LciSLcNgZjM3dAwVkkPFgTnu1gvFSR6HAb78d8cx4khpf5bvtPpRxLk1vXeqRxBagUQwgaTBJUEAbxF9bfs7TQBzboKL5krjpqXKydYQb5pbZ64bRk6U6hzNjmn8XEexRE8RACaJrk9MvtXVVqJRyKRkWRvMKgo3QtfXR1qNZcXKV5AHzwCPfHXfxXAkSkEPPXFxYyj33KLuuLAw7LyWqL1v6fFXzohq1oZB3tqXGZbksri3i86cCtMoUFUCUHT2BgCxUmQVGnuUvNTe69PU5VKraGowqUo8MJFXS7njsW7uZyK867qZpPPXwu2fLgK2whDihx6JKY1Ub3aw3mUkjbdqhMxSh4m6ZhD2piJYwtiEb7juZhrhVWVqaJDFCc2aED8Vdc1Z4W9pEt8UKhHcLcHn6CiaSHiyhFwRY3bMuVZwtY2XFwzQRSfW2VyYoC5Dqby43xYuEHe1Ld7ANMsXgHzwwkisdt4rQ1EHMynXM6926DEVYewgxrGEaXdYDymLWZ5DKHdmyvYW6keH9NysQGKytiax8wUyiqyaLhXoqbJCWxrcyu9MZLZpJFRXonKCNK1SAmRkRNQfkmY9jDVnj5nBdjtEGxa9pwAFENv6xa58J1zcA4MD6Tj9B4B36YrWT2TLs7Pp4jMJryA5sUVsL6jn7cEnbjaH78qQw2sicncneBaAoQnFJedyWfnQC3GYRAfRPp3kxfZNsJf2aQQ").get
  val HOUR: Long = 60 * 60 * 1000L
  var prover: ErgoProver = _
  val MAX_MINER_FEE = 1e7.toLong
  val TEAM_FEE = 3
  val AUC_BOX_INIT_VAL = 1e7.toLong

  val lpnft = ErgoId.create("d968ce08fd24f3f5fad86f9dded2eaf0920b63c6fd62e56489f74789a8807c2a")

  val LP_ID = "3593b7134d51f706a7f511a4d255bdc3d0c792ce11b63d8ea64f7c84bc6b4721"


  val initialTime: Long = 1698215400000L
  //1698215329930
//  val initialTime: Long = 18215400000L
  val duration: Long = HOUR * 24 * 3
  val period: Long = HOUR * 24 * 4

  def compileContracts(ctx: BlockchainContext): Unit = {
    val auctionHash = Blake2b256.hash(auctionAddr.script.bytes)

    val buyBackContract = ctx.compileContract(ConstantsBuilder.create()
      .item("CONFIG_LP", lpnft.getBytes)
      .item("TEAM_ADDRESS", teamAddr.script.bytes)
      .item("ACNFT", acNFT.getBytes)
      .item("MAX_MINER_FEE", MAX_MINER_FEE)
      .item("TEAM_FEE", TEAM_FEE)
      .build(), Contracts.buyBack)
    buyBackAddr = addrEnc.fromProposition(buyBackContract.getErgoTree).get
    println("Buy back contract address: " + addrEnc.fromProposition(buyBackContract.getErgoTree).get)

    val buyBackContractHash = Blake2b256.hash(buyBackContract.getErgoTree.bytes)
    var contract = ctx.compileContract(ConstantsBuilder.create()
      .item("CONFIG_LP", lpnft.getBytes)
      .item("AuctionContractHash", auctionHash)
      .item("BuyBackContractHash", buyBackContractHash)
      .item("MAX_MINER_FEE", MAX_MINER_FEE)
      .item("AUC_BOX_INIT_VAL", AUC_BOX_INIT_VAL)
      .build(), Contracts.main)
    acAddr = addrEnc.fromProposition(contract.getErgoTree).get
    println("Main contract address: " + addrEnc.fromProposition(contract.getErgoTree).get)

  }

  def getAcBoxOut(ctx: BlockchainContext, auctionInfo: Seq[Seq[Long]], contract: ErgoAddress,
                  ergVal: Long = 1e9.toLong, tokVal: Long = 100000, duration: Long = Main.duration,
                  period: Long = Main.period, startTime: Long = Main.initialTime): OutBox = {
    val seqColl = auctionInfo.map(r => ErgoValue.of(r.toArray).getValue).toArray
    val auctions = ErgoValue.of(seqColl, ErgoType.collType(ErgoType.longType()))

    val txB = ctx.newTxBuilder()
    val out = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(contract.script, NetworkType.MAINNET))
      .value(ergVal)
      .tokens(new ErgoToken(acNFT, 1), new ErgoToken(acId, tokVal))
      .registers(ErgoValue.of(Seq(startTime, period).toArray), auctions)
      .build()
    out
  }

  def getAcBox(ctx: BlockchainContext, auctionInfo: Seq[Seq[Long]], contract: ErgoAddress,
               ergVal: Long = 1e9.toLong, tokVal: Long = 100000, duration: Long = Main.duration,
               period: Long = Main.period, startTime: Long = Main.initialTime): InputBox = {
    val seqColl = auctionInfo.map(r => ErgoValue.of(r.toArray).getValue).toArray
    val auctions = ErgoValue.of(seqColl, ErgoType.collType(ErgoType.longType()))

    val txB = ctx.newTxBuilder()
    val out = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(contract.script, NetworkType.MAINNET))
      .value(ergVal)
      .tokens(new ErgoToken(acNFT, 1), new ErgoToken(acId, tokVal))
      .registers(ErgoValue.of(Seq(startTime, period).toArray), auctions)
      .build()
    out.convertToInputWith("f36a52dd37aaafe8dc601659f610dcf06d7d04ef778f148c9e734d7c50c67840", 0)
  }


  def getBuyBackBox(ctx: BlockchainContext, value:Long=1e9.toLong): InputBox = {
    val txB = ctx.newTxBuilder()
    val out = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(buyBackAddr.script, NetworkType.MAINNET))
      .value(value)
      .build()
    out.convertToInputWith("f36a52dd37aaafe8dc601659f610dcf06d7d04ef778f148c9e734d7c50c67840", 0)
  }

  def getBuyBackBoxFailed(ctx: BlockchainContext, value: Long = 100): InputBox = {
    val txB = ctx.newTxBuilder()
    val out = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(buyBackAddr.script, NetworkType.MAINNET))
      .value(1e7.toLong)
      .tokens(new ErgoToken(acId, value))
      .build()
    out.convertToInputWith("f36a52dd37aaafe8dc601659f610dcf06d7d04ef778f148c9e734d7c50c67840", 0)
  }

  def startAuctionTest(ctx: BlockchainContext): Unit = {
    val curTime = ctx.createPreHeader().build().getTimestamp
//    val auctionInfo: Seq[Seq[Long]] = Seq(Seq(100L, HOUR * 24L, 500L), Seq(100L, HOUR * 24L, 500L))
    val duration = 3 * HOUR
    val auctionInfo: Seq[Seq[Long]] = Seq(Seq(5, duration, 500L, 1052L), Seq(4, duration, 500L, 1052L), Seq(3, duration, 500L, 1052L))
    val cntr = "{sigmaProp(true)}"
    val compiled = ctx.compileContract(ConstantsBuilder.empty(), cntr)
    var addr = addrEnc.fromProposition(compiled.getErgoTree).get
    addr = acAddr
//    val ac = getAcBox(ctx, auctionInfo, addr)
    val ac = getAcBox(ctx, auctionInfo, addr, tokVal = 1000L, ergVal = 1e9.toLong)
//    val ac = ctx.getBoxesById("8250c7da5151412d90ed70bae84639f72bee58988fced8aa17625b4bf5315105").head

    val lp = ctx.getBoxesById(LP_ID).head
    val txB = ctx.newTxBuilder()
    val ins = Seq(ac)


    val aucStartInfo = ac.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray

    val minerFee = 1e6.toLong
    val smAuction = auctionInfo.map(a => a.head).sum
    val prevTime = ac.getRegisters.get(0).getValue.asInstanceOf[Coll[Long]].toArray.head
    println(prevTime, curTime, curTime - prevTime)
    val outAc = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(ac.getErgoTree, NetworkType.MAINNET))
      .value(ac.getValue - auctionInfo.size * AUC_BOX_INIT_VAL - minerFee)
      .tokens(ac.getTokens.get(0), new ErgoToken(acId, ac.getTokens.get(1).getValue - smAuction))
//      .registers(ErgoValue.of(Seq(prevTime + HOUR, HOUR).toArray), ac.getRegisters.get(1))
      .registers(ErgoValue.of(Seq(aucStartInfo(0) + aucStartInfo(1), aucStartInfo(1)).toArray), ac.getRegisters.get(1))
      .build()
    var outs = Seq[OutBox](outAc)
    val stPrice = lp.getValue / lp.getTokens.get(2).getValue
    auctionInfo.foreach(info => {
      val curPrice = stPrice * info.head
      val curStPrice = (curPrice * 1000) / info(2)
      val endPrice = (curPrice * 1000) / info(3)
      val numDec = info(1) / HOUR - 1
      val step = (curStPrice - endPrice) / numDec

      val auc = txB.outBoxBuilder()
        .contract(new ErgoTreeContract(auctionAddr.script, NetworkType.MAINNET))
        .value(AUC_BOX_INIT_VAL)
        .tokens(new ErgoToken(acId, info.head))
        .registers(ErgoValue.of(buyBackAddr.script.bytes), ErgoValue.pairOf(ErgoValue.of(curTime + HOUR / 2), ErgoValue.of(curTime + HOUR / 2 + info(1))),
          ErgoValue.of(Seq(curStPrice, step, HOUR).toArray), ErgoValue.of(Seq[Byte]().toArray))
        .build()
      outs = outs :+ auc
    })

    val tx = txB.boxesToSpend(ins.asJava)
      .addOutputs(outs: _*)
      .fee(minerFee)
      .sendChangeTo(teamAddr)
      .addDataInputs(lp)
      .build()
    prover.sign(tx)
  }

  def returnTest(ctx: BlockchainContext): Unit = {
    val auctionInfo: Seq[Seq[Long]] = Seq(Seq(100L, HOUR * 24L, 500L), Seq(100L, HOUR * 24L, 500L))
    val cntr = "{sigmaProp(true)}"
    val compiled = ctx.compileContract(ConstantsBuilder.empty(), cntr)
    var addr = addrEnc.fromProposition(compiled.getErgoTree).get
    addr = acAddr
    val ac = getAcBox(ctx, auctionInfo, addr)
    val buyBackBox = getBuyBackBoxFailed(ctx)

    val lp = ctx.getBoxesById(LP_ID).head
    val txB = ctx.newTxBuilder()
    val ins = Seq(buyBackBox, ac)

    val minerFee = 1e6.toLong
    val outAc = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(acAddr.script, NetworkType.MAINNET))
      .value(ac.getValue + (buyBackBox.getValue - minerFee))
      .tokens(ac.getTokens.get(0), new ErgoToken(acId, ac.getTokens.get(1).getValue + buyBackBox.getTokens.get(0).getValue))
      .registers(ac.getRegisters.asScala.toSeq: _*)
      .build()

    val tx = txB.boxesToSpend(ins.asJava)
      .addOutputs(outAc)
      .fee(minerFee)
      .sendChangeTo(teamAddr)
      .addDataInputs(lp)
      .build()
    prover.sign(tx)
  }



  def buyBackTest(ctx: BlockchainContext): Unit = {
    val duration = 3 * HOUR
    val auctionInfo: Seq[Seq[Long]] = Seq(Seq(5, duration, 500L, 1052L), Seq(4, duration, 500L, 1052L), Seq(3, duration, 500L, 1052L))

    val cntr = "{sigmaProp(true)}"
    val compiled = ctx.compileContract(ConstantsBuilder.empty(), cntr)
    var addr = addrEnc.fromProposition(compiled.getErgoTree).get
    addr = acAddr
    val ac = getAcBox(ctx, auctionInfo, addr)
    val buyBackBox = getBuyBackBox(ctx)

    val lp = ctx.getBoxesById(LP_ID).head
    val txB = ctx.newTxBuilder()
    val ins = Seq(lp, ac, buyBackBox)

    val yRes = lp.getValue.toBigInt
    val xRes = lp.getTokens.get(2).getValue
    val lpFee = lp.getRegisters.get(0).getValue.asInstanceOf[Int].toLong
    val funds = buyBackBox.getValue - MAX_MINER_FEE - (buyBackBox.getValue * TEAM_FEE) / 100
    val willGet = ((xRes * funds * lpFee) / (yRes * 1000L + funds * lpFee)).toLong

    val minerFee = 1e6.toLong
    val newAcVal = 1e7.toLong - minerFee + ac.getValue
    val newAcTokenVal = ac.getTokens.get(1).getValue + willGet
    val outAc = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(acAddr.script, NetworkType.MAINNET))
      .value(newAcVal)
      .tokens(ac.getTokens.get(0), new ErgoToken(acId, newAcTokenVal))
      .registers(ac.getRegisters.asScala.toSeq: _*)
      .build()
    val outLp = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(lp.getErgoTree, NetworkType.MAINNET))
      .value(lp.getValue + funds)
      .tokens(lp.getTokens.get(0), lp.getTokens.get(1), new ErgoToken(acId, lp.getTokens.get(2).getValue - willGet))
      .registers(lp.getRegisters.asScala.toSeq: _*)
      .build()

    val teamOut = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(teamAddr.script, NetworkType.MAINNET))
      .value((buyBackBox.getValue * TEAM_FEE) / 100)
      .build()

    val tx = txB.boxesToSpend(ins.asJava)
      .addOutputs(outLp, outAc, teamOut)
      .fee(minerFee)
      .sendChangeTo(teamAddr)
      .addDataInputs(lp)
      .build()
    prover.sign(tx)
  }

  def createAc(ctx: BlockchainContext): Unit = {
    val auctionInfo: Seq[Seq[Long]] = Seq(
      Seq(50, duration, 500L, 1052L),
      Seq(50, duration, 500L, 1052L),
      Seq(200, duration, 500L, 1052L),
      Seq(100, duration, 500L, 1052L),
      Seq(100, duration, 500L, 1052L),
      Seq(100, duration, 500L, 1052L),
      Seq(100, duration, 500L, 1052L),
      Seq(100, duration, 500L, 1052L),
      Seq(100, duration, 500L, 1052L),
      Seq(100, duration, 500L, 1052L),
    )

    val inp = ctx.getBoxesById("5f00a6543f7a33f3c155f61bb67ed0154cf622058c8fa3520eb6ea93d30c2101").head
//    val inp2 = ctx.getBoxesById("405bf011fafcaa645beff9e416ff1264bc6df1269f297c2352a80d4393ac1c7b").head

    val ac = getAcBoxOut(ctx, auctionInfo, acAddr, tokVal = 89000, ergVal = 20e9.toLong)

    val txB = ctx.newTxBuilder()
//    val ins = Seq(inp, inp2)
    val ins = Seq(inp)

    val minerFee = 5e6.toLong
    val tx = txB.boxesToSpend(ins.asJava)
      .addOutputs(ac)
      .fee(minerFee)
      .sendChangeTo(USER_P2PK_ADDRESS)
      .build()
    val signed = prover.sign(tx)
    println(ctx.sendTransaction(signed))
  }


  def main(args: Array[String]): Unit = {
    // use appkit to compile the contract
    ergoClient.execute(ctx => {
      compileContracts(ctx)
      prover = ctx.newProverBuilder().withDLogSecret(BigInt.apply(USER_SECRET).bigInteger).build()

//      buyBackTest(ctx)
//      returnTest(ctx)
//      startAuctionTest(ctx)
//      createAc(ctx)

    })


  }
}