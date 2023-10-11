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
  val ergoClient: ErgoClient = RestApiErgoClient.create("http://128.140.55.130:9053/", NetworkType.MAINNET, "", "https://api.ergoplatform.com/api/v1/")
  val addrEnc = new ErgoAddressEncoder(NetworkType.MAINNET.networkPrefix)

  // for testing
  val USER_P2PK_ADDRESS: ErgoAddress = Address.create("9gN7YssQgnRzDDXHAuamAwgikRkzeLYq8Jn9YvsPcKD13ajj33u").getErgoAddress
  val USER_SECRET = "19899857734851286010002701428515536889175392016268936110507684368097105859366"

  var buyBackAddr: ErgoAddress = _
  var acAddr: ErgoAddress = _

  val acId = "aee8132a6602dd215dac8d1caf973581277614e267702a770f45d7ffe5234cba"
  val lpnft = ErgoId.create("915da2ac421906919351163a9afa1f17918272750987906b00247e91925e757d")
  val teamAddr = addrEnc.fromString("9evruWoFfhTZ4q4Xc4whQK4bJMqMAF7z9YeygzLvCHMgR17Pt5n").get
  val acNFT = ErgoId.create("02194927b8d96833067052f40f032039d55c9d9f87a2ca47b7e4d56497de0f58")
  val auctionAddr = addrEnc.fromString("quE2HvwgAp7z8q4tD1BfCfj72TPziUHqruk3L8jUWQ7bWGD7ugdkpkgYx9kztPTmJborN4baTrHuenaZrCtoB3VHeLjQUQACzZ67qpnMGUAyGs93mUpwodKekvSM8Y6rCFVMxFw76MD8ZUjMPd2J8awcfECurL9v6HkjFnzKJr72NXdQ18XYYGRneXbbkgU5qzUKTLoC2weUGsDpNhfDh9xBGV7CYconikP79c9b3ZcquSBdNCu4G5r2uvBVTHXaFgk5TioeSL4SqwS5sugeEeicTPre842uZBzNd7B3XqE75Rjum1bz1vBgGtadMqt2XRui7tqaK44LjeYfds3N4CWZsYwx69h2PpHFrU9Mzvvj3ju6XGVS5X6GnyGgttXiHgokZPhD45m2etzAUXovNPeutrw5zmQudCh9jXBSfujXDBPS7ohC7qLj3Hj4zfRJTmUzPNMEUGLED57kowMXdAq95h4LciSLcNgZjM3dAwVkkPFgTnu1gvFSR6HAb78d8cx4khpf5bvtPpRxLk1vXeqRxBagUQwgaTBJUEAbxF9bfs7TQBzboKL5krjpqXKydYQb5pbZ64bRk6U6hzNjmn8XEexRE8RACaJrk9MvtXVVqJRyKRkWRvMKgo3QtfXR1qNZcXKV5AHzwCPfHXfxXAkSkEPPXFxYyj33KLuuLAw7LyWqL1v6fFXzohq1oZB3tqXGZbksri3i86cCtMoUFUCUHT2BgCxUmQVGnuUvNTe69PU5VKraGowqUo8MJFXS7njsW7uZyK867qZpPPXwu2fLgK2whDihx6JKY1Ub3aw3mUkjbdqhMxSh4m6ZhD2piJYwtiEb7juZhrhVWVqaJDFCc2aED8Vdc1Z4W9pEt8UKhHcLcHn6CiaSHiyhFwRY3bMuVZwtY2XFwzQRSfW2VyYoC5Dqby43xYuEHe1Ld7ANMsXgHzwwkisdt4rQ1EHMynXM6926DEVYewgxrGEaXdYDymLWZ5DKHdmyvYW6keH9NysQGKytiax8wUyiqyaLhXoqbJCWxrcyu9MZLZpJFRXonKCNK1SAmRkRNQfkmY9jDVnj5nBdjtEGxa9pwAFENv6xa58J1zcA4MD6Tj9B4B36YrWT2TLs7Pp4jMJryA5sUVsL6jn7cEnbjaH78qQw2sicncneBaAoQnFJedyWfnQC3GYRAfRPp3kxfZNsJf2aQQ").get
  val HOUR: Long = 60 * 60 * 1000L
  var prover: ErgoProver = _
  val MAX_MINER_FEE = 1e7.toLong
  val TEAM_FEE = 2
  val AUC_BOX_INIT_VAL = 1e7.toLong

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

  def getUserBox(ctx: BlockchainContext, tokens: List[ErgoToken] = List.empty, value: Long = 1e9.toLong, script: ErgoTree = USER_P2PK_ADDRESS.script): InputBox = {
    val txB = ctx.newTxBuilder()
    val out = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(script, NetworkType.MAINNET))
      .value(value)
      .build()
    out.convertToInputWith("f36a52dd37aaafe8dc601659f610dcf06d7d04ef778f148c9e734d7c50c67840", 0)
  }

  def getAcBox(ctx: BlockchainContext, auctionInfo: Seq[Seq[Long]], contract: ErgoAddress): InputBox = {
    val seqColl = auctionInfo.map(r => ErgoValue.of(r.toArray).getValue).toArray
    val auctions = ErgoValue.of(seqColl, ErgoType.collType(ErgoType.longType()))

    val txB = ctx.newTxBuilder()
    val out = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(contract.script, NetworkType.MAINNET))
      .value(1e9.toLong)
      .tokens(new ErgoToken(acNFT, 1), new ErgoToken(acId, 100000))
      .registers(ErgoValue.of(Seq(0L, HOUR).toArray), auctions)
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
    val auctionInfo: Seq[Seq[Long]] = Seq(Seq(100L, HOUR * 24L, 500L), Seq(100L, HOUR * 24L, 500L))
    val cntr = "{sigmaProp(true)}"
    val compiled = ctx.compileContract(ConstantsBuilder.empty(), cntr)
    var addr = addrEnc.fromProposition(compiled.getErgoTree).get
    addr = acAddr
    val ac = getAcBox(ctx, auctionInfo, addr)

    val lp = ctx.getBoxesById("57533c42c447eac4cff38d7ed8407d3ee85914b3bd1e6daa53332f08ebcd19d0").head
    val txB = ctx.newTxBuilder()
    val ins = Seq(ac)

    val minerFee = 1e6.toLong
    val smAuction = auctionInfo.map(a => a.head).sum
    val outAc = txB.outBoxBuilder()
      .contract(new ErgoTreeContract(acAddr.script, NetworkType.MAINNET))
      .value(ac.getValue - auctionInfo.size * AUC_BOX_INIT_VAL - minerFee)
      .tokens(ac.getTokens.get(0), new ErgoToken(acId, ac.getTokens.get(1).getValue - smAuction))
      .registers(ErgoValue.of(Seq(HOUR, HOUR).toArray), ac.getRegisters.get(1))
      .build()
    var outs = Seq[OutBox](outAc)
    val stPrice = lp.getValue / lp.getTokens.get(2).getValue
    auctionInfo.foreach(info => {
      val curPrice = stPrice * info.head
      println(curPrice/1e9)
      val coef = 1000 / info(2)
      val curStPrice = curPrice * coef
      val numDec = info(1) / HOUR
      val step = (curStPrice - curPrice) / numDec

      val auc = txB.outBoxBuilder()
        .contract(new ErgoTreeContract(auctionAddr.script, NetworkType.MAINNET))
        .value(AUC_BOX_INIT_VAL)
        .tokens(new ErgoToken(acId, info.head))
        .registers(ErgoValue.of(buyBackAddr.script.bytes), ErgoValue.pairOf(ErgoValue.of(curTime + HOUR / 2), ErgoValue.of(curTime + info(1))),
          ErgoValue.of(Seq(curStPrice, -step, HOUR).toArray), ErgoValue.of(Seq[Byte]().toArray))
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

    val lp = ctx.getBoxesById("57533c42c447eac4cff38d7ed8407d3ee85914b3bd1e6daa53332f08ebcd19d0").head
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
    val auctionInfo: Seq[Seq[Long]] = Seq(Seq(100L, HOUR * 24L, 500L), Seq(100L, HOUR * 24L, 500L))


    val cntr = "{sigmaProp(true)}"
    val compiled = ctx.compileContract(ConstantsBuilder.empty(), cntr)
    var addr = addrEnc.fromProposition(compiled.getErgoTree).get
    addr = acAddr
    val ac = getAcBox(ctx, auctionInfo, addr)
    val buyBackBox = getBuyBackBox(ctx)

    val lp = ctx.getBoxesById("57533c42c447eac4cff38d7ed8407d3ee85914b3bd1e6daa53332f08ebcd19d0").head
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


  def main(args: Array[String]): Unit = {
    // use appkit to compile the contract
    ergoClient.execute(ctx => {
      compileContracts(ctx)
      prover = ctx.newProverBuilder().withDLogSecret(BigInt.apply(USER_SECRET).bigInteger).build()

      buyBackTest(ctx)
      returnTest(ctx)
      startAuctionTest(ctx)

    })


  }
}