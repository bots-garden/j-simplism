package bots.garden.j_simplism;

import com.dylibso.chicory.runtime.Memory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.runtime.Instance;

import java.io.File;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    System.out.println("📦 Loading wasm file...");

    File wasmFile = new File("./hello.wasi.wasm");

    Module module = Module.build(wasmFile);

    // if wasm: Can't find opcode for op value 236
    // if wasi: Can't find opcode for op value 204

    Instance instance = module.instantiate();

    ExportFunction helloFunction = instance.getExport("hello");
    // https://github.com/dylibso/chicory?tab=readme-ov-file#memory-and-complex-types
    ExportFunction alloc = instance.getExport("alloc");
    ExportFunction dealloc = instance.getExport("dealloc");

    vertx.createHttpServer().requestHandler(req -> {

      // todo: manage concurrent access
      Memory memory = instance.getMemory();
      String message = "Bob Morane";
      int len = message.getBytes().length;
      // allocate {len} bytes of memory, this returns a pointer to that memory
      int ptr = alloc.apply(Value.i32(len))[0].asInt();
      // We can now write the message to the module's memory:
      memory.put(ptr, message);

      Value result = helloFunction.apply(Value.i32(ptr), Value.i32(len))[0];
      dealloc.apply(Value.i32(ptr), Value.i32(len));

      req.response()
        .putHeader("content-type", "text/plain")
        .end(result.toString());

    }).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
