#!/usr/bin/python
#
#  Copyright 2002-2018 Barcelona Supercomputing Center (www.bsc.es)
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# 
def matmul(mSize, nSize, kSize, bSize, debug):
    # Initialize
    a = initialize(mSize, nSize, bSize, True)
    b = initialize(nSize, kSize, bSize, True)
    c = initialize(mSize, kSize, bSize, False)

    # Debug
    if debug:
        print("Matrix A:")
        print(a)
        print("Matrix B:")
        print(b)
        print("Matrix C:")
        print(c)

    # Perform computation
    # c = a*b
    for i in range(mSize):
        for j in range(kSize):
            for k in range(nSize):
                c[i][j] += a[i][k] * b[k][j]

    # Debug
    if debug:
        print("Matrix C:")
        print(c)

    # Result
    return c
