#!/bin/bash

prog="$(command -v "$0")"
while [ -L "$prog" ]; do
  prog="$(readlink -f "$prog")"
done

file="$(dirname "$prog")/src/com/computernerd1101/goban/internal/GobanRows.kt"

function writeCode() {
  cat <<EOT
@file:Suppress("UNCHECKED_CAST", "unused")

package com.computernerd1101.goban.internal

import java.util.concurrent.atomic.AtomicLongFieldUpdater

internal object GobanRows {
    @JvmField val init = arrayOf<() -> GobanRows1>(
EOT
  for n in {1..52}; do
    (( n2=2*n ))
    if [ $n -lt 52 ]; then
      n2=$n2,
    fi
    echo "        ::GobanRows$n, ::GobanRows$n2"
  done
  cat <<EOT
    )
    @JvmField val updaters = arrayOf(
EOT
  n=0
  while [ $n -lt 102 ]; do
    i=$n
    (( n++ ))
    j=$n
    (( n++ ))
    if [ $n -le 52 ]; then
      m=$j
    else
      m=$n
    fi
    echo "        GobanRows$m.update$i, GobanRows$n.update$j,"
  done
  cat <<EOT
        GobanRows104.update102, GobanRows104.update103
    )
}
internal open class GobanRows1 {
    open val size: Int get() = 1
    @Volatile private var row0: Long = 0L
    operator fun get(index: Int) = GobanRows.updaters[index][this]
    operator fun set(index: Int, value: Long) {
        GobanRows.updaters[index][this] = value
    }
    companion object {
        @JvmField internal val update0 = atomicLongUpdater<GobanRows1>("row0")
    }
}
EOT
  n=1
  while [ $n -lt 52 ]; do
    i=$n
    (( n++ ))
    cat <<EOT
internal open class GobanRows$n: GobanRows$i() {
    override val size: Int get() = $n
    @Volatile private var row$i: Long = 0L
    companion object {
        @JvmField
        internal val update$i = atomicLongUpdater<GobanRows$n>("row$i") as AtomicLongFieldUpdater<GobanRows1>
    }
}
EOT
  done
  mod=' open'
  while [ $n -lt 104 ]; do
    i=$n
    (( n++ ))
    j=$n
    (( n++ ))
    if [ $n -eq 104 ]; then
      mod=
    fi
    cat <<EOT
internal$mod class GobanRows$n: GobanRows$i() {
    override val size: Int get() = $n
    @Volatile private var row$i: Long = 0L
    @Volatile private var row$j: Long = 0L
    companion object {
        @JvmField
        internal val update$i = atomicLongUpdater<GobanRows$n>("row$i") as AtomicLongFieldUpdater<GobanRows1>
        @JvmField
        internal val update$j = atomicLongUpdater<GobanRows$n>("row$j") as AtomicLongFieldUpdater<GobanRows1>
    }
}
EOT
  done

}

writeCode >"$file"