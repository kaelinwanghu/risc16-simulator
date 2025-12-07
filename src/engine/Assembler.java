package engine;

import engine.types.Register;
import engine.types.Instruction;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.lang.reflect.Method;

public final class Assembler {

	// Assembler should not be initialized, given that it is essentially a static class
	private Assembler() {
		throw new IllegalStateException("Utility class");
	}

	public static void assemble(String program, Processor processor) {
		String noInstructionsString = "Please enter one or more instructions";
		if (program.trim().isEmpty()) {
			throw new IllegalArgumentException(noInstructionsString);
		}
		
		// Resetting static variables
		processor.clear();
		tags.clear();
		instructionAddress = 0;
		
		String[] lines = program.toLowerCase().trim().split("\\n+");
		boolean hasInstruction = false;
		for (String line : lines)
		{
			// Remove comments if there are any for each line, and trim it
			String cleanLine = preprocessLine(line);
			// If the line still has something, parse it and indicate that instructions exist
			if (!cleanLine.isEmpty()) {
				hasInstruction = true;
				System.out.println("cleanLine2: " + cleanLine);
				parseInstruction(cleanLine, processor);
			}
		}
		if (!hasInstruction) {
			throw new IllegalArgumentException(noInstructionsString);
		}
		
		resolveSymbolicLabels(processor);
		for (Map.Entry<String, Integer> entry : tags.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue().toString());
		}
				
	}

	/**
	 * Proprocesses a line by removing comments and getting/stripping the label from a line (if it exists)
	 * @param line the line to process
	 * @return the post-processed line, ready to be added to the program
	 */
	private static String preprocessLine(String line) {
		// Remove comment first
		String cleanLine = removeComment(line).trim();
		if (cleanLine.isEmpty()) {
			return cleanLine;
		}

		System.out.println("cleanLine: " + cleanLine);
		// Check if a label is present.
		if (cleanLine.contains(":")) {
			String[] parts = cleanLine.split(":", 2);
			String label = parts[0].trim();
			System.out.println("label: " + label);
			if (label.isEmpty()) {
				throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Empty label");
			}
			else if (!checkLabelValidity(label)) {
				throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Labels can only contain alphanumeric symbols and \'.\' or \'_\'");
			}
			if (parts[1].trim().isEmpty()) {
				throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Cannot have a line with just a label");
			}

			// Record the label with the current instruction address 
			tags.put(label, instructionAddress);
			cleanLine = parts[1].trim();
			System.out.println("rest: " + cleanLine);
		}
		instructionAddress += 2; // 2 bytes per instruction
		return cleanLine;
	}

	private static boolean checkLabelValidity(String label) {
		int labelLength = label.length();
		for (int i = 0; i < labelLength; ++i) {
			char c = label.charAt(i);
			// alphanumeric or . or _ check
			if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && (c != '.') && (c != '_')) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes the comment section of a line (anything that goes after the '#' character)
	 * @param line the line string to be parsed
	 * @return the line without the comment section
	 */
	private static String removeComment(String line) {
        int commentIndex = line.indexOf('#');
        return (commentIndex >= 0) ? line.substring(0, commentIndex) : line;
    }
	
	private static void parseInstruction(String instruction, Processor processor) {
		String operation;
		String[] operands = {};
		if (instruction.indexOf(' ') != -1) {
			operation = instruction.substring(0, instruction.indexOf(' '));
			operands = instruction.substring(instruction.indexOf(' ') + 1).split(",");
		}
		else {
			operation = instruction;
		}
		System.out.println("operation: " + operation);
		for (int i = 0; i < operands.length; i++) {
			operands[i] = operands[i].trim();
		}
		// If the instruction is a pseudo-instruction, then parse that and return
		if (pseudoInstructions.keySet().contains(operation)) {
			parsePseudoInstruction(operation, operands, processor);
			return;
		}
		Method m = InstructionSet.getMethod(operation);
		if (m == null) {
			throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + operation + " is an invalid operation");
		}
		Class<?>[] types = m.getParameterTypes();
		if (types.length != operands.length) 
			throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Invalid operands number");
		
		Object[] parameters = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i] == Register.class) {
				Register r = processor.getRegisterFile().getRegister(operands[i]);
				if (r == null) {
					throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + operands[i] + " is an invalid register name");
				}
				parameters[i] = r;
			} else if (types[i] == int.class) {
				Integer immediate = parseIntegerNoThrow(operands[i]); 
				if (immediate != null)
				{
					if (operation.equals("lui") && (immediate < 0 || immediate > 0x3ff)) {
						throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Upper immediate must be a value between 0x000 and 0x3ff");
					}
					else if (!operation.equals("lui") && (immediate < -64 || immediate > 63)) {
						throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Signed immediate must be a value between -64 and 63");
					}
					parameters[i] = immediate;
				}
				else
				{
					if (operation.equals("beq")) {
                        parameters[i] = operands[i];
					}
					else {
						throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Invalid immediate operand: " + operands[i]);
					}
				}
			}
		}
		processor.getMemory().addInstruction(operation, parameters);
	}
	
	public static int parseInteger(String number) {
		try {
			if (number.matches("-?\\d+")) 
				return Integer.parseInt(number);
			if (number.matches("0[xX][\\da-fA-F]+"))
				return Integer.parseInt(number.substring(2), 16);
		} catch (Exception e) {
			throw new IllegalCallerException("Failed to parse integer");
		}
		throw new IllegalArgumentException(number + " is an invalid integer");
	}

	public static Integer parseIntegerNoThrow(String number) {
		try {
			if (number.matches("-?\\d+")) 
				return Integer.parseInt(number);
			if (number.matches("0[xX][\\da-fA-F]+"))
				return Integer.parseInt(number.substring(2), 16);
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	public static short parseShort(String number) {
		try {
			if (number.matches("-?\\d+")) 
				return Short.parseShort(number);
			if (number.matches("0[xX][\\da-fA-F]+"))
				return Short.parseShort(number.substring(2), 16);
		} catch (Exception e) {
			throw new IllegalCallerException("Failed to parse short");
		}
		throw new IllegalArgumentException(number + " is an invalid short");
	}

	/**
	 * Pseudo-instruction parser that formulates the pseudo-instructions into actual instructions and then adds them to the processor
	 * @param operation the pseudo-operation to be performed
	 * @param operands the operands of the pseudo-operation
	 * @param processor the processor (necessary to call back to add instructions)
	 */
	private static void parsePseudoInstruction(String operation, String[] operands, Processor processor)
	{
		// Do a quick check to ensure that the operands number is valid
		if (pseudoInstructions.get(operation) != operands.length) {
			throw new IllegalArgumentException("Invalid operands number");
		}
		StringBuilder actualInstructions = new StringBuilder();
		switch (operation) {
			case "nop": {
				// add r0, r0, r0 triggers a write to r0 error, so r1 it is for now
				actualInstructions.append("add r0, r0, r0");
				parseInstruction(actualInstructions.toString(), processor);
				break;
			}
			// TODO: think of a solution to this later (maybe jank it)
			case "halt": {
				actualInstructions.append("jalr r0, r0");
				parseInstruction(actualInstructions.toString(), processor);
				break;
			}
			case "lli": {
				// lli really translates down to addi with a mask of only the first 6 bits of the immediate
				actualInstructions.append("addi " + operands[0] + ", " + operands[0] + ", ");
				actualInstructions.append((parseInteger(operands[1]) & 0x3f));
				parseInstruction(actualInstructions.toString(), processor);
				break;
			}
			case "movi": {
				// movi is an lui + lli pair, which necessitates 2 instructions and thus 2 stringbuilder parses
				int immediate = parseInteger(operands[1]);
				// Check immediate to make sure it is within bounds
				if (immediate < 0 || immediate > 0xFFFF)
				{
					throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Word immediate must be a value between 0x0000 and 0xfff");
				}
				// Append lui and shift the operand back 6 so it can be correctly shifted by lui
				actualInstructions.append("lui " + operands[0] + ", ");
				actualInstructions.append(immediate >> 6);
				parseInstruction(actualInstructions.toString(), processor);

				// Reset stringbuilder
				actualInstructions.setLength(0);

				// And then append addi (lli) with the first 6 bits to be parsed
				actualInstructions.append("addi " + operands[0] + ", " + operands[0] + ", ");
				actualInstructions.append(immediate & 0x3f);
				parseInstruction(actualInstructions.toString(), processor);

				instructionAddress += 2; // account for the extra instruction
				break;
			}
			// .fill and .space are not compatible with this version of the RISC, since it has a dedicated memory space
			case ".fill": {

				break;
			}
			case ".space": {
				break;
			}
			default: {
				throw new IllegalCallerException("Line " + instructionAddress / 2 + ": " + "Invalid pseudo-instruction");
			}
		}
	}

    private static void resolveSymbolicLabels(Processor processor) {
        ArrayList<Instruction> instructions = processor.getMemory().getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            Object[] operands = instr.getOperands();
            int currentAddress = i * 2;
            for (int j = 0; j < operands.length; j++) {
				System.out.println("operand " + j + ": " + operands[j] + " with type: " + operands[j].getClass());
                if (operands[j] instanceof String label) {
                    if (!tags.containsKey(label)) {
						throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Undefined label");
					}
                    int targetAddress = tags.get(label);
                    int offset = targetAddress - currentAddress - 2;
                    if (offset < -64 || offset > 63)
					{
						throw new IllegalArgumentException("Line " + instructionAddress / 2 + ": " + "Branch offset out of range");
					}
                    operands[j] = offset;
                }
            }
        }
    }

	// Assembler variable to keep track of the number of instructions (good for error displays and label resolving)
	private static int instructionAddress = 0;
	
	private static HashMap<String, Integer> tags = new HashMap<>();
	// The pseudoInstructions to check against as well as how many operands they have (Java object instantiation sucks)
	private static final HashMap<String, Integer> pseudoInstructions = new HashMap<>(Map.of("nop", 0, "halt", 0, "lli", 2, "movi", 2, ".fill", 1, ".space", 1));
}