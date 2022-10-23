package io.dmarkwat.twitter.finagle

import com.twitter.util.{ExecutorServiceFuturePool, FuturePools}

import java.util.concurrent.{ExecutorService, Executors}

trait PoolSupport {
  def executor(): ExecutorService = Executors.newFixedThreadPool(10)
}

trait PoolSupport1 extends PoolSupport {

  lazy val primary: ExecutorServiceFuturePool = FuturePools.newFuturePool(executor())
}

trait PoolSupport2 extends PoolSupport1 {

  lazy val secondary: ExecutorServiceFuturePool = FuturePools.newFuturePool(executor())
}

trait PoolSupport3 extends PoolSupport2 {

  lazy val tertiary: ExecutorServiceFuturePool = FuturePools.newFuturePool(executor())
}
